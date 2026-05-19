package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.BackingType;
import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import com.pourchoices.chordpro.application.domain.model.ChordProPath;
import com.pourchoices.chordpro.application.domain.model.Setlist;
import com.pourchoices.chordpro.application.domain.model.SetlistAssignment;
import com.pourchoices.chordpro.application.domain.model.SetlistEntry;
import com.pourchoices.chordpro.application.port.in.AssignBackingTrackSlotsUseCase;
import com.pourchoices.chordpro.application.port.out.CatalogPort;
import com.pourchoices.chordpro.application.port.out.SetlistAssignmentsPort;
import com.pourchoices.chordpro.application.port.out.SetlistPort;
import com.pourchoices.chordpro.config.ChordproCatalogIndexPathConfig;
import com.pourchoices.chordpro.config.ChordproSetlistAssignmentsPathConfig;
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
 * Assigns RC-500 backing-track slot numbers to all songs that have a set designation
 * for the target gig and a real backing track (BACKING non-blank and not sentinel "99").
 *
 * <h3>Slot allocation rules</h3>
 * <ul>
 *   <li><b>In-set songs</b> (SET prefix A–Y) — sorted by SET code, slots starting at
 *       {@value #IN_SET_START_SLOT}.</li>
 *   <li><b>Backup songs</b> (SET prefix Z) — sorted alphabetically by title, slots
 *       starting at {@value #BACKUP_START_SLOT}, capped at {@value #MAX_SLOT}.</li>
 *   <li>Songs with no backing track (blank or "99") are included in the setlist but
 *       skipped during slot assignment.</li>
 * </ul>
 *
 * <h3>Side-effects</h3>
 * <ol>
 *   <li>Writes the updated {@code song-catalog.csv} (all entries, not just set songs).</li>
 *   <li>Calls {@link UpdateSongService} for every song whose BACKING value changed,
 *       pushing the new slot into the individual {@code .cho} file.</li>
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
    private final ChordproCatalogIndexPathConfig catalogConfig;
    private final ChordproSetlistAssignmentsPathConfig assignmentsConfig;
    private final SetlistDeduplicator deduplicator;
    private final SetlistJoiner joiner;
    private final UpdateSongService updateSongService;

    @Override
    public Setlist assignSlots(String gigParam, String outputPath) {

        // ── 1. Load catalog and assignments ─────────────────────────────────
        Path catalogPath = Paths.get(catalogConfig.getCatalogIndexPath());
        Map<String, CatalogEntry> catalogMap = new HashMap<>(catalogPort.readCatalogFromCsv(catalogPath));
        log.info("Loaded {} total catalog entries", catalogMap.size());

        Path assignmentsPath = Paths.get(assignmentsConfig.getSetlistAssignmentsPath());
        List<SetlistAssignment> allAssignments = assignmentsPort.readAssignments(assignmentsPath);
        log.info("Loaded {} total assignment(s)", allAssignments.size());

        // ── 2. Resolve gig, join catalog + assignments, de-duplicate ─────────
        String resolvedGig = joiner.resolveGig(gigParam, allAssignments);
        List<SetlistEntry> joined = joiner.join(gigParam, allAssignments, catalogMap);
        List<SetlistEntry> deduped = deduplicator.deduplicate(joined);
        log.info("{} set-assigned entries after de-duplication for gig '{}'",
                deduped.size(), resolvedGig);

        // ── 3. Split into in-set (A–Y prefix) vs. backup (Z prefix) ─────────
        List<SetlistEntry> inSet = deduped.stream()
                .filter(e -> !e.getSet().toUpperCase().startsWith("Z"))
                .sorted(Comparator.comparing(SetlistEntry::getSet))
                .toList();

        List<SetlistEntry> backup = deduped.stream()
                .filter(e -> e.getSet().toUpperCase().startsWith("Z"))
                .sorted(Comparator.comparing(SetlistEntry::getTitle))
                .toList();

        log.info("In-set: {} songs, Backup (Z-set): {} songs", inSet.size(), backup.size());

        // ── 4. Assign slots ──────────────────────────────────────────────────
        Map<String, CatalogEntry> updated = new HashMap<>();

        int inSetSlot = IN_SET_START_SLOT;
        for (SetlistEntry entry : inSet) {
            if (!hasBacking(entry)) continue;
            String newSlot = String.valueOf(inSetSlot++);
            if (!newSlot.equals(entry.getBacking())) {
                updated.put(entry.getSongId().toString(),
                        entry.getSong().toBuilder().rcSlot(newSlot).build());
            }
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
            String newSlot = String.valueOf(backupSlot++);
            if (!newSlot.equals(entry.getBacking())) {
                updated.put(entry.getSongId().toString(),
                        entry.getSong().toBuilder().rcSlot(newSlot).build());
            }
        }
        log.info("Assigned backup slots {} – {}", BACKUP_START_SLOT, backupSlot - 1);
        log.info("{} songs had their backing-track slot changed", updated.size());

        // ── 5. Merge changes into the full catalog map ───────────────────────
        updated.forEach(catalogMap::put);

        // ── 6. Write the updated full catalog to CSV ─────────────────────────
        List<CatalogEntry> allEntries = new ArrayList<>(catalogMap.values());
        allEntries.sort(Comparator.comparing(e -> e.getSongId().toString()));
        catalogPort.writeCatalogToCsv(catalogPath, allEntries);
        log.info("song-catalog.csv updated with new backing-track slot assignments");

        // ── 7. Push changes into each affected .cho file ─────────────────────
        for (String songIdStr : updated.keySet()) {
            String filePath = ChordProPath.toFilePath(updated.get(songIdStr).getSongId());
            log.info("Updating .cho file: {}", filePath);
            updateSongService.updateSong(filePath);
        }

        // ── 8. Re-join updated catalog with assignments to build final setlist
        List<SetlistEntry> finalJoined = joiner.join(resolvedGig, allAssignments, catalogMap);
        List<SetlistEntry> setlistEntries = deduplicator.deduplicate(finalJoined).stream()
                .sorted(Comparator.comparing(SetlistEntry::getSet))
                .toList();

        Setlist setlist = Setlist.builder()
                .gig(resolvedGig)
                .entries(setlistEntries)
                .build();

        // ── 9. Write the setlist CSV ──────────────────────────────────────────
        setlistPort.writeSetlistToCsv(Paths.get(outputPath), setlistEntries);
        log.info("setlist.csv written with {} songs to {}", setlist.size(), outputPath);

        return setlist;
    }

    private boolean hasBacking(SetlistEntry entry) {
        return entry.getBackingType() == BackingType.RC;
    }
}
