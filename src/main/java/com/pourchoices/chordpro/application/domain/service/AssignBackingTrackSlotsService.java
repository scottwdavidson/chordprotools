package com.pourchoices.chordpro.application.domain.service;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Assigns RC-500 backing-track slot numbers for a specific gig.
 *
 * <h3>Slot allocation rules</h3>
 * <ul>
 *   <li><b>In-set songs</b> (SET prefix A–Y) — sorted by SET code, slots starting at
 *       {@value #IN_SET_START_SLOT}.</li>
 *   <li><b>Backup songs</b> (SET prefix Z) — sorted alphabetically by title, slots
 *       starting at {@value #BACKUP_START_SLOT}, capped at {@value #MAX_SLOT}.</li>
 *   <li>Songs without an RC backing track are included in the setlist but skipped.</li>
 * </ul>
 *
 * <h3>Side-effects</h3>
 * <ol>
 *   <li>Writes the RC SLOT values for this gig back into {@code gigs.csv}
 *       (other gigs are untouched).</li>
 *   <li>Patches {@code {meta: rc-slot: N}} directly into each affected {@code .cho}
 *       file — the catalog is not written.</li>
 *   <li>Writes a fresh {@code setlist.csv} via {@link SetlistPort}.</li>
 * </ol>
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
    public Setlist assignSlots(String gigParam, String outputPath) {

        // ── 1. Load catalog and assignments ─────────────────────────────────
        Path catalogPath  = Paths.get(catalogConfig.getCatalogIndexPath());
        Path gigsPath     = Paths.get(gigsConfig.getGigsPath());

        Map<String, CatalogEntry>  catalogMap     = catalogPort.readCatalogFromCsv(catalogPath);
        List<SetlistAssignment>    allAssignments = assignmentsPort.readAssignments(gigsPath);
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

        // ── 4. Assign slots → build map of songId → new slot ────────────────
        Map<String, String> newSlots = new HashMap<>();

        int inSetSlot = IN_SET_START_SLOT;
        for (SetlistEntry entry : inSet) {
            if (!hasBacking(entry)) continue;
            newSlots.put(entry.getSongId().toString(), String.valueOf(inSetSlot++));
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
            newSlots.put(entry.getSongId().toString(), String.valueOf(backupSlot++));
        }
        log.info("Assigned backup slots {} – {}", BACKUP_START_SLOT, backupSlot - 1);
        log.info("{} songs received a backing-track slot", newSlots.size());

        // ── 5. Update gigs.csv: write RC SLOT into this gig's rows ──────────
        List<SetlistAssignment> updatedAssignments = allAssignments.stream()
                .map(a -> {
                    if (!resolvedGig.equals(a.getGig())) return a;
                    String slot = newSlots.get(a.getSongId().toString());
                    return a.toBuilder().rcSlot(slot).build();
                })
                .toList();
        assignmentsPort.writeAssignments(gigsPath, updatedAssignments);
        log.info("gigs.csv updated with RC SLOT assignments for gig '{}'", resolvedGig);

        // ── 6. Patch {meta: rc-slot: N} directly into each .cho file ────────
        newSlots.forEach((songIdStr, slot) -> {
            String filePath = ChordProPath.toFilePath(
                    updatedAssignments.stream()
                            .filter(a -> songIdStr.equals(a.getSongId().toString()))
                            .findFirst()
                            .orElseThrow()
                            .getSongId());
            log.info("Patching rc-slot={} into {}", slot, filePath);
            patchRcSlotInFile(Paths.get(filePath), slot);
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

        return setlist;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private boolean hasBacking(SetlistEntry entry) {
        return entry.getBackingType() == BackingType.RC;
    }

    /**
     * Reads the {@code .cho} file, replaces (or adds) the
     * {@code {meta: rc-slot: N}} directive, and writes it back.
     * All other content is preserved unchanged.
     */
    private void patchRcSlotInFile(Path filePath, String slot) {
        List<String> lines = chordProPort.read(filePath);
        ParsedSong parsed   = songParser.parse(filePath.toString(), lines);
        ParsedHeader oldHeader = parsed.getParsedHeader();

        // Rebuild header: strip any existing RC_SLOT line, add the new one
        ParsedHeader.ParsedHeaderBuilder builder = ParsedHeader.builder()
                .chordProFilename(oldHeader.getChordProFilename());
        oldHeader.getHeaderLines().stream()
                .filter(l -> l.getHeaderDirective() != HeaderDirective.RC_SLOT)
                .forEach(builder::headerLine);
        builder.headerLine(ParsedHeaderLine.builder()
                .headerDirective(HeaderDirective.RC_SLOT)
                .value(slot)
                .build());

        ParsedSong patched = parsed.withHeader(builder.build());
        chordProPort.write(filePath, patched);
    }
}
