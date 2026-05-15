package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import com.pourchoices.chordpro.application.domain.model.ChordProPath;
import com.pourchoices.chordpro.application.domain.model.Setlist;
import com.pourchoices.chordpro.application.port.in.AssignBackingTrackSlotsUseCase;
import com.pourchoices.chordpro.application.port.out.CatalogPort;
import com.pourchoices.chordpro.application.port.out.SetlistPort;
import com.pourchoices.chordpro.config.ChordproCatalogIndexPathConfig;
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
 * Assigns RC-500 backing-track slot numbers to all songs that have a Set designation
 * and a real backing track (i.e., BACKING is not blank and not the sentinel "99").
 *
 * <h3>Slot allocation rules</h3>
 * <ul>
 *   <li><b>In-set songs</b> (SET prefix A–Y, e.g. A01, B03, C11) — sorted by SET code,
 *       slots assigned sequentially starting at {@value #IN_SET_START_SLOT}.</li>
 *   <li><b>Backup songs</b> (SET prefix Z, e.g. Z0, Z1) — sorted alphabetically by title,
 *       slots assigned sequentially starting at {@value #BACKUP_START_SLOT}, capped at
 *       {@value #MAX_SLOT}.</li>
 *   <li>Songs with no backing track (blank or "99" BACKING) are included in the setlist
 *       but skipped during slot assignment — their BACKING value is not modified.</li>
 * </ul>
 *
 * <h3>Side-effects</h3>
 * <ol>
 *   <li>Writes the updated {@code song-catalog.csv} (all entries, not just set songs).</li>
 *   <li>Calls {@link UpdateSongService} for every song whose BACKING value changed,
 *       pushing the new value into the individual {@code .cho} file.</li>
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

    /** Sentinel value indicating a song has no backing track. */
    private static final String NO_BACKING_SENTINEL = "99";

    private final CatalogPort catalogPort;
    private final SetlistPort setlistPort;
    private final ChordproCatalogIndexPathConfig config;
    private final SetlistDeduplicator deduplicator;
    private final UpdateSongService updateSongService;

    @Override
    public Setlist assignSlots(String outputPath) {

        // ── 1. Load the full catalog ─────────────────────────────────────────
        Path catalogPath = Paths.get(config.getCatalogIndexPath());
        Map<String, CatalogEntry> catalogMap = new HashMap<>(catalogPort.readCatalogFromCsv(catalogPath));
        log.info("Loaded {} total catalog entries", catalogMap.size());

        // ── 2. Filter to set-assigned songs, then de-duplicate ───────────────
        List<CatalogEntry> withSet = catalogMap.values().stream()
                .filter(e -> e.getSet() != null && !e.getSet().isBlank())
                .toList();
        List<CatalogEntry> deduped = deduplicator.deduplicate(withSet);
        log.info("{} set-assigned entries after de-duplication", deduped.size());

        // ── 3. Split into in-set (A–Y prefix) vs. backup (Z prefix) ─────────
        List<CatalogEntry> inSet = deduped.stream()
                .filter(e -> !e.getSet().toUpperCase().startsWith("Z"))
                .sorted(Comparator.comparing(CatalogEntry::getSet))
                .toList();

        List<CatalogEntry> backup = deduped.stream()
                .filter(e -> e.getSet().toUpperCase().startsWith("Z"))
                .sorted(Comparator.comparing(CatalogEntry::getTitle))
                .toList();

        log.info("In-set: {} songs, Backup (Z-set): {} songs", inSet.size(), backup.size());

        // ── 4. Assign slots ──────────────────────────────────────────────────
        Map<String, CatalogEntry> updated = new HashMap<>();

        int inSetSlot = IN_SET_START_SLOT;
        for (CatalogEntry entry : inSet) {
            if (!hasBacking(entry)) continue;
            String newSlot = String.valueOf(inSetSlot++);
            if (!newSlot.equals(entry.getBacking())) {
                updated.put(entry.getSongId().toString(), entry.toBuilder().backing(newSlot).build());
            }
        }
        log.info("Assigned in-set slots {} – {}", IN_SET_START_SLOT, inSetSlot - 1);

        int backupSlot = BACKUP_START_SLOT;
        for (CatalogEntry entry : backup) {
            if (!hasBacking(entry)) continue;
            if (backupSlot > MAX_SLOT) {
                log.warn("RC-500 slot limit ({}) reached — skipping backup song '{}'", MAX_SLOT, entry.getTitle());
                continue;
            }
            String newSlot = String.valueOf(backupSlot++);
            if (!newSlot.equals(entry.getBacking())) {
                updated.put(entry.getSongId().toString(), entry.toBuilder().backing(newSlot).build());
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

        // ── 8. Build the final setlist (with updated backing values) ─────────
        List<CatalogEntry> finalSetlistEntries = allEntries.stream()
                .filter(e -> e.getSet() != null && !e.getSet().isBlank())
                .sorted(Comparator.comparing(CatalogEntry::getSet))
                .toList();

        // Re-run dedup on the refreshed entries to produce the clean setlist
        List<CatalogEntry> setlistEntries = deduplicator.deduplicate(finalSetlistEntries).stream()
                .sorted(Comparator.comparing(CatalogEntry::getSet))
                .toList();

        Setlist setlist = Setlist.builder().entries(setlistEntries).build();

        // ── 9. Write the setlist CSV ──────────────────────────────────────────
        setlistPort.writeSetlistToCsv(Paths.get(outputPath), setlistEntries);
        log.info("setlist.csv written with {} songs to {}", setlist.size(), outputPath);

        return setlist;
    }

    /**
     * Returns {@code true} when a song has a real backing track on the RC-500 —
     * i.e., BACKING is non-blank and not the no-backing sentinel {@value #NO_BACKING_SENTINEL}.
     */
    private boolean hasBacking(CatalogEntry entry) {
        String b = entry.getBacking();
        return b != null && !b.isBlank() && !NO_BACKING_SENTINEL.equals(b);
    }
}
