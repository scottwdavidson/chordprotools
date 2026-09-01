package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.AssignBackingTrackSlotsResult;
import com.pourchoices.chordpro.application.domain.model.BackingType;
import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import com.pourchoices.chordpro.application.domain.model.ChordProPath;
import com.pourchoices.chordpro.application.domain.model.HeaderDirective;
import com.pourchoices.chordpro.application.domain.model.ParsedHeader;
import com.pourchoices.chordpro.application.domain.model.ParsedHeaderLine;
import com.pourchoices.chordpro.application.domain.model.ParsedSong;
import com.pourchoices.chordpro.application.domain.model.Setlist;
import com.pourchoices.chordpro.application.domain.model.SetlistAssignment;
import com.pourchoices.chordpro.application.domain.model.SetlistEntry;
import com.pourchoices.chordpro.application.domain.model.SongId;
import com.pourchoices.chordpro.application.port.in.AssignBackingTrackSlotsUseCase;
import com.pourchoices.chordpro.application.port.out.CatalogPort;
import com.pourchoices.chordpro.application.port.out.ChordProPort;
import com.pourchoices.chordpro.application.port.out.SetlistAssignmentsPort;
import com.pourchoices.chordpro.application.port.out.SetlistPort;
import com.pourchoices.chordpro.config.ChordproCatalogIndexPathConfig;
import com.pourchoices.chordpro.config.ChordproGigsPathConfig;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Assigns RC-500 backing-track slot numbers for a specific gig.
 *
 * <h3>Default mode: preserve and sync</h3>
 * <ul>
 *   <li>Any RC-backed song that already has a non-blank RC SLOT in {@code gigs.csv}
 *       for this gig - set by a previous run <em>or typed in by hand</em> - is left
 *       completely untouched.</li>
 *   <li>RC-backed songs with no slot yet get the next free number, filling gaps
 *       before extending upward, walking the setlist in the same order as before
 *       (in-set by SET code, then backup by title).</li>
 *   <li>Every currently-assigned slot (preserved or new) is synced into its
 *       {@code .cho} file's {@code {meta: rc-slot}} directive, annotated with this
 *       gig's name - but only written if the value actually changed.</li>
 *   <li>Before anything is written, {@code gigs.csv} is validated for this gig: every
 *       non-blank RC SLOT must be a number 1–{@value #MAX_SLOT}, and no two different
 *       songs may share the same slot. Either problem aborts with nothing written.</li>
 * </ul>
 *
 * <h3>{@code reoptimize} mode</h3>
 * Ignores whatever is already in {@code gigs.csv} and fully recomputes every slot
 * from scratch — the original behavior, for when a genuine renumber is worth it.
 *
 * <h3>Slot ranges (both modes)</h3>
 * <ul>
 *   <li><b>In-set songs</b> (SET prefix A–Y) — slots starting at {@value #IN_SET_START_SLOT}.</li>
 *   <li><b>Backup songs</b> (SET prefix Z) — slots starting at {@value #BACKUP_START_SLOT},
 *       capped at {@value #MAX_SLOT}.</li>
 *   <li>Songs without an RC backing track are skipped, and any stray RC SLOT value
 *       they carry is cleared.</li>
 * </ul>
 */
@Service
@AllArgsConstructor(onConstructor_ = @__(@Autowired))
@Slf4j
public class AssignBackingTrackSlotsService implements AssignBackingTrackSlotsUseCase {

    /** First RC-500 slot reserved for in-set backing tracks. Slots 1–4 are left free. */
    static final int IN_SET_START_SLOT = 5;

    /** First RC-500 slot reserved for backup (Z-set) backing tracks. */
    static final int BACKUP_START_SLOT = 50;

    /** Highest slot the RC-500 supports. */
    static final int MAX_SLOT = 99;

    private final CatalogPort catalogPort;
    private final SetlistPort setlistPort;
    private final SetlistAssignmentsPort assignmentsPort;
    private final ChordProPort chordProPort;
    private final SongParser songParser;
    private final ChordproCatalogIndexPathConfig catalogConfig;
    private final ChordproGigsPathConfig gigsConfig;
    private final SetlistDeduplicator deduplicator;
    private final SetlistJoiner joiner;

    @Override
    public AssignBackingTrackSlotsResult assignSlots(String gigParam, String outputPath, boolean reoptimize) {

        // ── 1. Load catalog and assignments ─────────────────────────────────
        Path catalogPath = Paths.get(catalogConfig.getCatalogIndexPath());
        Path gigsPath    = Paths.get(gigsConfig.getGigsPath());

        Map<String, CatalogEntry> catalogMap     = catalogPort.readCatalogFromCsv(catalogPath);
        List<SetlistAssignment>   allAssignments = assignmentsPort.readAssignments(gigsPath);
        log.info("Loaded {} catalog entries and {} assignment(s)",
                catalogMap.size(), allAssignments.size());

        // ── 2. Resolve gig, join, de-duplicate ──────────────────────────────
        String resolvedGig = joiner.resolveGig(gigParam, allAssignments);
        List<SetlistEntry> deduped = deduplicator.deduplicate(
                joiner.join(gigParam, allAssignments, catalogMap));
        log.info("{} set-assigned entries for gig '{}'", deduped.size(), resolvedGig);

        // ── 3. Split in-set (A–Y) vs. backup (Z) ────────────────────────────
        List<SetlistEntry> inSet = deduped.stream()
                .filter(e -> !e.getSet().toUpperCase().startsWith("Z"))
                .sorted(Comparator.comparing(SetlistEntry::getSet))
                .toList();

        List<SetlistEntry> backup = deduped.stream()
                .filter(e -> e.getSet().toUpperCase().startsWith("Z"))
                .sorted(Comparator.comparing(SetlistEntry::getTitle))
                .toList();

        log.info("In-set: {} songs, Backup (Z-set): {} songs", inSet.size(), backup.size());

        // ── 4. Compute the slot plan ─────────────────────────────────────────
        SlotPlan plan = reoptimize ? planReoptimized(inSet, backup) : planPreserved(inSet, backup);

        // ── 5. Update gigs.csv: write RC SLOT into this gig's rows ──────────
        List<SetlistAssignment> updatedAssignments = allAssignments.stream()
                .map(a -> {
                    if (!resolvedGig.equals(a.getGig())) return a;
                    String slot = plan.slots().get(a.getSongId().toString());
                    return a.toBuilder().rcSlot(slot).build();
                })
                .toList();
        assignmentsPort.writeAssignments(gigsPath, updatedAssignments);
        log.info("gigs.csv updated with RC SLOT assignments for gig '{}'", resolvedGig);

        // ── 6. Sync {meta: rc-slot: N (gig)} into every affected .cho file ───
        plan.slots().forEach((songIdStr, slot) -> {
            SongId songId = updatedAssignments.stream()
                    .filter(a -> songIdStr.equals(a.getSongId().toString()))
                    .findFirst()
                    .orElseThrow()
                    .getSongId();
            syncSlotIntoFile(songId, slot, resolvedGig);
        });

        // ── 7. Re-join with updated assignments to build final setlist ───────
        List<SetlistEntry> setlistEntries = deduplicator
                .deduplicate(joiner.join(resolvedGig, updatedAssignments, catalogMap))
                .stream()
                .sorted(Comparator.comparing(SetlistEntry::getSet))
                .toList();

        Setlist setlist = Setlist.builder()
                .gig(resolvedGig)
                .entries(setlistEntries)
                .build();

        // ── 8. Write setlist.csv ─────────────────────────────────────────────
        setlistPort.writeSetlistToCsv(Paths.get(outputPath), setlistEntries);
        log.info("setlist.csv written with {} songs to {}", setlist.size(), outputPath);

        return AssignBackingTrackSlotsResult.builder()
                .setlist(setlist)
                .preservedCount(plan.preservedCount())
                .newlyAssignedCount(plan.newlyAssignedCount())
                .reoptimized(reoptimize)
                .build();
    }

    // ── Slot planning ────────────────────────────────────────────────────────

    private record SlotPlan(Map<String, String> slots, int preservedCount, int newlyAssignedCount) {}

    /** Ignores gigs.csv entirely; recomputes every RC-backed song's slot from scratch. */
    private SlotPlan planReoptimized(List<SetlistEntry> inSet, List<SetlistEntry> backup) {
        Map<String, String> slots = new LinkedHashMap<>();

        int inSetSlot = IN_SET_START_SLOT;
        for (SetlistEntry entry : inSet) {
            if (!hasBacking(entry)) continue;
            slots.put(entry.getSongId().toString(), String.valueOf(inSetSlot++));
        }
        log.info("Assigned in-set slots {} – {}", IN_SET_START_SLOT, inSetSlot - 1);

        int backupSlot = BACKUP_START_SLOT;
        for (SetlistEntry entry : backup) {
            if (!hasBacking(entry)) continue;
            if (backupSlot > MAX_SLOT) {
                log.warn("RC-500 slot limit ({}) reached — skipping backup song '{}'",
                        MAX_SLOT, entry.getTitle());
                continue;
            }
            slots.put(entry.getSongId().toString(), String.valueOf(backupSlot++));
        }
        log.info("{} songs received a backing-track slot (full recompute)", slots.size());

        return new SlotPlan(slots, 0, slots.size());
    }

    /**
     * Leaves every already-slotted RC-backed song untouched; assigns new slots
     * only to RC-backed songs with no current value, filling gaps first.
     */
    private SlotPlan planPreserved(List<SetlistEntry> inSet, List<SetlistEntry> backup) {
        List<SetlistEntry> rcBacked = Stream.concat(inSet.stream(), backup.stream())
                .filter(this::hasBacking)
                .toList();

        Map<String, String> existing = new LinkedHashMap<>();
        for (SetlistEntry entry : rcBacked) {
            String slot = entry.getAssignment().getRcSlot();
            if (slot != null && !slot.isBlank()) {
                existing.put(entry.getSongId().toString(), slot.trim());
            }
        }

        validateExistingSlots(existing);

        Set<Integer> occupied = existing.values().stream()
                .map(Integer::parseInt)
                .collect(Collectors.toCollection(HashSet::new));

        Map<String, String> newlyAssigned = new LinkedHashMap<>();

        int inSetSlot = IN_SET_START_SLOT;
        for (SetlistEntry entry : inSet) {
            if (!hasBacking(entry)) continue;
            String songIdStr = entry.getSongId().toString();
            if (existing.containsKey(songIdStr)) continue;

            inSetSlot = nextFreeSlot(inSetSlot, occupied);
            if (inSetSlot > MAX_SLOT) {
                log.warn("RC-500 slot limit ({}) reached — skipping in-set song '{}'",
                        MAX_SLOT, entry.getTitle());
                continue;
            }
            newlyAssigned.put(songIdStr, String.valueOf(inSetSlot));
            occupied.add(inSetSlot);
            inSetSlot++;
        }

        int backupSlot = BACKUP_START_SLOT;
        for (SetlistEntry entry : backup) {
            if (!hasBacking(entry)) continue;
            String songIdStr = entry.getSongId().toString();
            if (existing.containsKey(songIdStr)) continue;

            backupSlot = nextFreeSlot(backupSlot, occupied);
            if (backupSlot > MAX_SLOT) {
                log.warn("RC-500 slot limit ({}) reached — skipping backup song '{}'",
                        MAX_SLOT, entry.getTitle());
                continue;
            }
            newlyAssigned.put(songIdStr, String.valueOf(backupSlot));
            occupied.add(backupSlot);
            backupSlot++;
        }

        log.info("Preserved {} existing slot(s), assigned {} new slot(s)",
                existing.size(), newlyAssigned.size());

        Map<String, String> finalSlots = new LinkedHashMap<>(existing);
        finalSlots.putAll(newlyAssigned);
        return new SlotPlan(finalSlots, existing.size(), newlyAssigned.size());
    }

    /** Walks upward from {@code candidate} until it finds a slot not already claimed. */
    private int nextFreeSlot(int candidate, Set<Integer> occupied) {
        while (candidate <= MAX_SLOT && occupied.contains(candidate)) {
            candidate++;
        }
        return candidate;
    }

    /**
     * Refuses to guess when gigs.csv has ambiguous or conflicting RC SLOT data
     * for this gig: a non-numeric/out-of-range value, or two different songs
     * claiming the same slot. Throws with nothing written if either is found.
     */
    private void validateExistingSlots(Map<String, String> existing) {
        List<String> problems = new ArrayList<>();

        for (Map.Entry<String, String> e : existing.entrySet()) {
            try {
                int value = Integer.parseInt(e.getValue());
                if (value < 1 || value > MAX_SLOT) {
                    problems.add(String.format(
                            "song '%s' has RC SLOT '%s', which is outside the valid range (1-%d)",
                            e.getKey(), e.getValue(), MAX_SLOT));
                }
            } catch (NumberFormatException ex) {
                problems.add(String.format(
                        "song '%s' has RC SLOT '%s', which is not a number",
                        e.getKey(), e.getValue()));
            }
        }

        existing.entrySet().stream()
                .collect(Collectors.groupingBy(Map.Entry::getValue,
                        Collectors.mapping(Map.Entry::getKey, Collectors.toList())))
                .forEach((slot, songIds) -> {
                    if (songIds.size() > 1) {
                        problems.add(String.format(
                                "RC SLOT '%s' is claimed by multiple songs: %s", slot, songIds));
                    }
                });

        if (!problems.isEmpty()) {
            throw new IllegalArgumentException(
                    "Cannot assign backing-track slots — gigs.csv has " + problems.size()
                    + " problem(s) that need human review before proceeding:\n  - "
                    + String.join("\n  - ", problems)
                    + "\nFix these in gigs.csv, then re-run assign-backing-track-slots.");
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private boolean hasBacking(SetlistEntry entry) {
        return entry.getBackingType() == BackingType.RC;
    }

    /**
     * Syncs {@code {meta: rc-slot: N (gig)}} into the {@code .cho} file, skipping
     * the write entirely if the file already has exactly this value - so a
     * re-run against an unchanged gig touches zero files.
     */
    private void syncSlotIntoFile(SongId songId, String slot, String gigName) {
        Path filePath = Paths.get(ChordProPath.toFilePath(songId));
        List<String> lines = chordProPort.read(filePath);
        ParsedSong parsed = songParser.parse(filePath.toString(), lines);
        ParsedHeader oldHeader = parsed.getParsedHeader();

        String targetValue = slot + " (" + gigName + ")";

        Optional<ParsedHeaderLine> existingLine = oldHeader.getHeaderLines().stream()
                .filter(l -> l.getHeaderDirective() == HeaderDirective.RC_SLOT)
                .findFirst();

        if (existingLine.isPresent() && targetValue.equals(existingLine.get().getValue())) {
            log.debug("rc-slot already up to date in {} — skipping write", filePath);
            return;
        }

        ParsedHeader.ParsedHeaderBuilder builder = ParsedHeader.builder()
                .chordProFilename(oldHeader.getChordProFilename());
        oldHeader.getHeaderLines().stream()
                .filter(l -> l.getHeaderDirective() != HeaderDirective.RC_SLOT)
                .forEach(builder::headerLine);
        builder.headerLine(ParsedHeaderLine.builder()
                .headerDirective(HeaderDirective.RC_SLOT)
                .value(targetValue)
                .build());

        log.info("Syncing rc-slot={} into {}", targetValue, filePath);
        chordProPort.write(filePath, parsed.withHeader(builder.build()));
    }
}
