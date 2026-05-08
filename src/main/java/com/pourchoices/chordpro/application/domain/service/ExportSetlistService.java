package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import com.pourchoices.chordpro.application.domain.model.Setlist;
import com.pourchoices.chordpro.application.port.in.ExportSetlistUseCase;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads the full song catalog, filters to entries that have a non-blank "set" value,
 * sorts them by that value (lexicographic — e.g. A01, A02, B01), and writes the result
 * to a setlist CSV file.
 *
 * <p>The in-memory {@link Setlist} retains the full {@link CatalogEntry} data for
 * downstream use. The CSV projection is delegated entirely to {@link SetlistPort}.
 */
@Service
@AllArgsConstructor(onConstructor_ = @__(@Autowired))
@Slf4j
public class ExportSetlistService implements ExportSetlistUseCase {

    private final CatalogPort catalogPort;
    private final SetlistPort setlistPort;
    private final ChordproCatalogIndexPathConfig chordproCatalogIndexPathConfig;

    @Override
    public Setlist exportSetlist(String outputPathString) {

        // 1. Load the full catalog
        Path catalogPath = Paths.get(chordproCatalogIndexPathConfig.getCatalogIndexPath());
        Map<String, CatalogEntry> catalogMap = catalogPort.readCatalogFromCsv(catalogPath);
        log.info("Loaded {} total catalog entries from {}", catalogMap.size(), catalogPath);

        // 2. Filter to entries that belong to a set
        List<CatalogEntry> withSet = catalogMap.values().stream()
                .filter(entry -> entry.getSet() != null && !entry.getSet().isBlank())
                .toList();
        log.info("Found {} entries with a Set value (before de-duplication)", withSet.size());

        // 3. De-duplicate: when the standard file and a key-variant file both carry
        //    the same set code, keep only the standard (base) file.
        List<CatalogEntry> setlistEntries = deduplicate(withSet).stream()
                .sorted(Comparator.comparing(CatalogEntry::getSet))
                .toList();
        log.info("{} entries remain after de-duplication", setlistEntries.size());

        // 4. Wrap in the Setlist domain object
        Setlist setlist = Setlist.builder()
                .entries(setlistEntries)
                .build();

        // 5. Write the setlist CSV — the adapter owns the DTO projection
        Path outputPath = Paths.get(outputPathString);
        setlistPort.writeSetlistToCsv(outputPath, setlistEntries);
        log.info("Setlist ({} songs) written to {}", setlist.size(), outputPath.toAbsolutePath());

        return setlist;
    }

    // -------------------------------------------------------------------------
    // De-duplication helpers
    // -------------------------------------------------------------------------

    /**
     * De-duplicates entries that represent the same song assigned to the same set position.
     *
     * <p>File naming convention: standard key → {@code SongName.cho}; key variant →
     * {@code SongName-KEY.cho} (e.g., {@code MyLife-c.cho} for the C-major version).
     * A trailing segment is treated as a musical-key suffix when it matches
     * {@code -[a-gA-G][#b]?m?} (e.g., {@code -c}, {@code -am}, {@code -g#m}, {@code -bb}).
     *
     * <p>When two or more entries share the same (parent directory, base stem, set code),
     * only the standard (non-key-variant) file is retained.  If all entries in the group
     * are key variants, the first one wins and a warning is logged.
     */
    private List<CatalogEntry> deduplicate(List<CatalogEntry> entries) {
        // Key: "dir/baseStem:setCode" → chosen entry for that slot
        Map<String, CatalogEntry> chosen = new LinkedHashMap<>();

        for (CatalogEntry entry : entries) {
            String key = buildDedupeKey(entry);
            CatalogEntry existing = chosen.get(key);

            if (existing == null) {
                chosen.put(key, entry);
            } else {
                boolean entryIsBase    = isBaseVersion(entry.getChordProFilename());
                boolean existingIsBase = isBaseVersion(existing.getChordProFilename());

                if (entryIsBase && !existingIsBase) {
                    // Replace the keyed variant already in the map with the base version
                    log.info("De-dup: replacing keyed variant {} with base version {}",
                            existing.getChordProFilename(), entry.getChordProFilename());
                    chosen.put(key, entry);
                } else if (!entryIsBase && existingIsBase) {
                    // Existing is already the base — discard this keyed variant
                    log.info("De-dup: discarding keyed variant {} (base {} already selected)",
                            entry.getChordProFilename(), existing.getChordProFilename());
                } else {
                    // Both base, or both keyed variants — keep whichever arrived first, warn
                    log.warn("De-dup: ambiguous duplicates '{}' and '{}' share set '{}'; keeping first.",
                            existing.getChordProFilename(), entry.getChordProFilename(), entry.getSet());
                }
            }
        }

        return new ArrayList<>(chosen.values());
    }

    /**
     * Builds the de-duplication key as {@code "parentDir/baseStem:setCode"}.
     * Two entries collide when they are variants of the same song in the same set slot.
     */
    private String buildDedupeKey(CatalogEntry entry) {
        Path filePath = Paths.get(entry.getChordProFilename());
        String dir      = filePath.getParent() != null ? filePath.getParent().toString() : "";
        String baseStem = extractBaseStem(filePath.getFileName().toString());
        return dir + "/" + baseStem + ":" + entry.getSet();
    }

    /**
     * Strips the {@code .cho} extension and any trailing musical-key suffix from a filename.
     * Examples: {@code MyLife-c.cho} → {@code MyLife}, {@code HowLong-a.cho} → {@code HowLong},
     * {@code PianoMan-old.cho} → {@code PianoMan-old} (not a key suffix).
     *
     * <p>A key suffix matches {@code -[a-gA-G][#b]?m?} (i.e., a dash followed by a note
     * letter, an optional accidental, and an optional minor indicator).
     */
    private String extractBaseStem(String filename) {
        String stem = filename.replaceAll("(?i)\\.cho$", "");
        return stem.replaceAll("-[a-gA-G][#b]?m?$", "");
    }

    /**
     * Returns {@code true} when the file has no musical-key suffix — it is the
     * canonical, standard-key version of the song.
     */
    private boolean isBaseVersion(String chordProFilename) {
        String filename = Paths.get(chordProFilename).getFileName().toString();
        String stem     = filename.replaceAll("(?i)\\.cho$", "");
        return stem.equals(extractBaseStem(filename));
    }
}
