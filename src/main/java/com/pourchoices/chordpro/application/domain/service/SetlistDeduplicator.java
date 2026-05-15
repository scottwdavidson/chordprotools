package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * De-duplicates a list of {@link CatalogEntry} objects that may contain both a
 * base version and one or more key-variant versions of the same song.
 *
 * <p>Extracted from {@link ExportSetlistService} so that any service that needs
 * a clean, de-duplicated setlist view can reuse the same rules without duplicating
 * logic.
 *
 * <p>See {@link #deduplicate(List)} for the full decision matrix.
 */
@Component
@Slf4j
public class SetlistDeduplicator {

    /**
     * De-duplicates entries that are variants of the same song (same parent directory
     * and base stem) when more than one of those variants has a Set value assigned.
     *
     * <p>Decision rules (applied per group sharing the same dir + base stem):
     * <ul>
     *   <li><b>No collision</b> – only one entry in the group → keep as-is.</li>
     *   <li><b>Scenario A</b> – base + variant, same set code → keep base, drop variant [INFO].</li>
     *   <li><b>Scenario B</b> – only a variant has a set code → single-member group, keep.</li>
     *   <li><b>Scenario C</b> – base + variant, different set codes → keep base [WARN].</li>
     *   <li><b>Both variants, same set</b> – keep first [WARN].</li>
     *   <li><b>Both variants, different sets</b> – keep first [WARN].</li>
     * </ul>
     */
    public List<CatalogEntry> deduplicate(List<CatalogEntry> entries) {
        Map<String, List<CatalogEntry>> groups = new LinkedHashMap<>();
        for (CatalogEntry entry : entries) {
            groups.computeIfAbsent(entry.getSongId().toGroupKey(), k -> new ArrayList<>()).add(entry);
        }

        List<CatalogEntry> result = new ArrayList<>();
        for (List<CatalogEntry> group : groups.values()) {
            if (group.size() == 1) {
                result.add(group.get(0));
                continue;
            }

            List<CatalogEntry> bases    = group.stream().filter(e ->  e.getSongId().isBaseVersion()).toList();
            List<CatalogEntry> variants = group.stream().filter(e -> !e.getSongId().isBaseVersion()).toList();

            if (!bases.isEmpty()) {
                CatalogEntry base = bases.get(0);
                result.add(base);
                for (CatalogEntry variant : variants) {
                    if (variant.getSet().equals(base.getSet())) {
                        log.info("De-dup [A]: dropping keyed variant '{}' (set '{}') — base '{}' already covers this set position.",
                                variant.getSongId(), variant.getSet(), base.getSongId());
                    } else {
                        log.warn("De-dup [C]: ignoring keyed variant '{}' (set '{}') — base '{}' (set '{}') takes precedence.",
                                variant.getSongId(), variant.getSet(), base.getSongId(), base.getSet());
                    }
                }
            } else {
                CatalogEntry first = variants.get(0);
                result.add(first);
                boolean allSameSet = variants.stream().allMatch(v -> v.getSet().equals(first.getSet()));
                for (CatalogEntry other : variants.subList(1, variants.size())) {
                    if (allSameSet) {
                        log.warn("De-dup [both variants, same set]: '{}' and '{}' both carry set '{}'; keeping '{}'.",
                                first.getSongId(), other.getSongId(), first.getSet(), first.getSongId());
                    } else {
                        log.warn("De-dup [both variants, different sets]: '{}' (set '{}') and '{}' (set '{}') conflict; keeping '{}'.",
                                first.getSongId(), first.getSet(), other.getSongId(), other.getSet(), first.getSongId());
                    }
                }
            }
        }
        return result;
    }
}
