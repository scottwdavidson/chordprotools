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

        // 3. De-duplicate: when a base file and a key-variant share the same song
        //    (same directory + base stem), keep only the base and handle all set-code
        //    combinations per the rules in deduplicate().
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
     * De-duplicates entries that are variants of the same song (same parent directory
     * and base stem) when more than one of those variants has a Set value assigned.
     *
     * <p>File naming convention: standard key → {@code SongName.cho}; key variant →
     * {@code SongName-KEY.cho} (e.g., {@code MyLife-c.cho}).  A trailing segment is
     * treated as a musical-key suffix when it matches {@code -[a-gA-G][#b]?m?}.
     *
     * <p>Decision rules (applied per group of entries sharing the same dir + base stem):
     * <ul>
     *   <li><b>No collision</b> – only one entry in the group → keep as-is.</li>
     *   <li><b>Scenario A</b> – base + variant, same set code → keep base, drop variant (INFO).</li>
     *   <li><b>Scenario B</b> – only a variant has a set code (base has none / doesn't exist)
     *       → single-member group, falls through as «no collision».</li>
     *   <li><b>Scenario C</b> – base + variant, different set codes → keep base, log WARN
     *       that the variant is being ignored.</li>
     *   <li><b>Both variants, same set</b> – keep first encountered, log WARN (no base).</li>
     *   <li><b>Both variants, different sets</b> – keep first encountered, log WARN about
     *       the discrepancy and the absence of a base version.</li>
     * </ul>
     */
    private List<CatalogEntry> deduplicate(List<CatalogEntry> entries) {
        // Group by (parent dir, base stem) — set code intentionally excluded so that
        // base+variant pairs with *different* set codes still collide here.
        Map<String, List<CatalogEntry>> groups = new LinkedHashMap<>();
        for (CatalogEntry entry : entries) {
            groups.computeIfAbsent(buildGroupKey(entry), k -> new ArrayList<>()).add(entry);
        }

        List<CatalogEntry> result = new ArrayList<>();
        for (List<CatalogEntry> group : groups.values()) {
            if (group.size() == 1) {
                result.add(group.get(0)); // no collision — keep as-is
                continue;
            }

            List<CatalogEntry> bases    = group.stream()
                    .filter(e ->  isBaseVersion(e.getChordProFilename())).toList();
            List<CatalogEntry> variants = group.stream()
                    .filter(e -> !isBaseVersion(e.getChordProFilename())).toList();

            if (!bases.isEmpty()) {
                // A base version exists — it always wins.
                CatalogEntry base = bases.get(0);
                result.add(base);

                for (CatalogEntry variant : variants) {
                    if (variant.getSet().equals(base.getSet())) {
                        // Scenario A: same set code — expected, quiet drop.
                        log.info("De-dup [A]: dropping keyed variant '{}' (set '{}') — "
                                        + "base '{}' already covers this set position.",
                                variant.getChordProFilename(), variant.getSet(),
                                base.getChordProFilename());
                    } else {
                        // Scenario C: different set codes — base wins, variant ignored with WARN.
                        log.warn("De-dup [C]: ignoring keyed variant '{}' (set '{}') — "
                                        + "base '{}' (set '{}') takes precedence. "
                                        + "If this variant was meant for a different set, "
                                        + "assign the set value to the base file instead.",
                                variant.getChordProFilename(), variant.getSet(),
                                base.getChordProFilename(), base.getSet());
                    }
                }
            } else {
                // No base version — all entries are keyed variants.
                CatalogEntry first = variants.get(0);
                result.add(first);

                boolean allSameSet = variants.stream()
                        .allMatch(v -> v.getSet().equals(first.getSet()));

                for (CatalogEntry other : variants.subList(1, variants.size())) {
                    if (allSameSet) {
                        // Both variants, same set code.
                        log.warn("De-dup [both variants, same set]: '{}' and '{}' both carry set '{}' "
                                        + "and neither is the base version; keeping '{}'. "
                                        + "Consider assigning the set value to the base file.",
                                first.getChordProFilename(), other.getChordProFilename(),
                                first.getSet(), first.getChordProFilename());
                    } else {
                        // Both variants, different set codes — worst case.
                        log.warn("De-dup [both variants, different sets]: '{}' (set '{}') and '{}' (set '{}') "
                                        + "conflict and no base version exists; keeping '{}'. "
                                        + "Resolve by assigning the set value to the base file.",
                                first.getChordProFilename(), first.getSet(),
                                other.getChordProFilename(), other.getSet(),
                                first.getChordProFilename());
                    }
                }
            }
        }

        return result;
    }

    /**
     * Builds the grouping key as {@code "parentDir/baseStem"}.
     * The set code is deliberately excluded so variants with different set codes
     * are still recognised as siblings of the same song.
     */
    private String buildGroupKey(CatalogEntry entry) {
        Path filePath = Paths.get(entry.getChordProFilename());
        String dir      = filePath.getParent() != null ? filePath.getParent().toString() : "";
        String baseStem = extractBaseStem(filePath.getFileName().toString());
        return dir + "/" + baseStem;
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
