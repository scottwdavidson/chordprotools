package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import com.pourchoices.chordpro.application.domain.model.SongId;
import com.pourchoices.chordpro.application.domain.model.SongMatch;
import com.pourchoices.chordpro.application.port.in.FindSongIdUseCase;
import com.pourchoices.chordpro.application.port.out.CatalogPort;
import com.pourchoices.chordpro.config.ChordproCatalogIndexPathConfig;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Searches {@code song-catalog.csv} by a title-or-artist fragment and returns
 * one {@link SongMatch} per song (the base/standard-key version), annotated
 * with how many key variants exist.
 *
 * <h3>Grouping</h3>
 * Catalog entries are bucketed by {@link SongId#toGroupKey()} so a base file and
 * all its key variants resolve to the same song. This reuses the canonical
 * key-variant rule from {@link SongId} rather than duplicating the regex —
 * previously this logic lived (twice) in a Python shell script.
 *
 * <h3>Orphan handling</h3>
 * If a group has key variants but no base (standard-key) version, the first
 * variant is promoted as the representative and flagged as an orphan — a
 * data-integrity issue the user should fix before pasting the ID into a setlist.
 */
@Service
@AllArgsConstructor(onConstructor_ = @__(@Autowired))
@Slf4j
public class FindSongIdService implements FindSongIdUseCase {

    private final CatalogPort catalogPort;
    private final ChordproCatalogIndexPathConfig catalogConfig;

    /** Base entry + variant entries for a single song (group key). */
    private static final class Group {
        CatalogEntry base;
        final List<CatalogEntry> variants = new ArrayList<>();
    }

    @Override
    public List<SongMatch> findByFragment(String fragment) {
        String needle = fragment == null ? "" : fragment.toLowerCase();

        Map<String, CatalogEntry> catalog =
                catalogPort.readCatalogFromCsv(Paths.get(catalogConfig.getCatalogIndexPath()));

        // ── Bucket entries by group key (base SONG ID, no key suffix) ─────────
        Map<String, Group> groups = new LinkedHashMap<>();
        for (CatalogEntry entry : catalog.values()) {
            SongId id = entry.getSongId();
            Group g = groups.computeIfAbsent(id.toGroupKey(), k -> new Group());
            if (id.isBaseVersion()) {
                g.base = entry;
            } else {
                g.variants.add(entry);
            }
        }

        // ── Filter to fragment matches and build results ──────────────────────
        List<SongMatch> matches = new ArrayList<>();
        for (Map.Entry<String, Group> e : groups.entrySet()) {
            Group g = e.getValue();

            CatalogEntry representative = g.base;
            boolean orphan = false;
            if (representative == null) {
                if (g.variants.isEmpty()) {
                    continue; // defensive — a group always has at least one entry
                }
                representative = g.variants.get(0); // promote first variant
                orphan = true;
            }

            if (!fragmentMatches(representative, needle)) {
                continue;
            }

            String displayId = orphan
                    ? representative.getSongId().toString()  // variant's own (suffixed) ID
                    : e.getKey();                            // group key (base ID)

            matches.add(SongMatch.builder()
                    .title(representative.getTitle())
                    .artist(representative.getArtist())
                    .key(representative.getKey())
                    .displaySongId(displayId)
                    .variantCount(g.variants.size())
                    .orphan(orphan)
                    .build());
        }

        matches.sort(Comparator.comparing(m -> m.getTitle().toLowerCase()));
        log.info("find-song-id '{}' → {} match(es)", fragment, matches.size());
        return matches;
    }

    private static boolean fragmentMatches(CatalogEntry e, String needle) {
        return e.getTitle().toLowerCase().contains(needle)
                || e.getArtist().toLowerCase().contains(needle);
    }
}
