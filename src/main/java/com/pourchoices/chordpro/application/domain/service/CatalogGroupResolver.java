package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import com.pourchoices.chordpro.application.domain.model.SongId;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Resolves every catalog entry that shares a song's {@link SongId#toGroupKey()}
 * — i.e. the base (standard-key) file plus every key-variant (a capo or
 * keyboard-transpose alternate performed in a different key) of the same song.
 *
 * <p>Several commands need to fan an operation out across a whole song group
 * rather than touching a single file (metadata pushes, drift checks, RC-slot
 * syncing, …). Before this class existed, each of those commands re-implemented
 * the identical {@code toGroupKey()} filter — this is the one place that lookup
 * lives now.
 */
@Component
public class CatalogGroupResolver {

    /**
     * Returns every {@link CatalogEntry} in {@code catalogMap} whose SongId
     * shares {@code songId}'s group key, in catalog iteration order. Includes
     * {@code songId}'s own entry if present. Never {@code null}; empty if the
     * group has no catalog entries at all (e.g. an unknown song ID).
     */
    public List<CatalogEntry> findGroupVariants(SongId songId, Map<String, CatalogEntry> catalogMap) {
        String groupKey = songId.toGroupKey();
        return catalogMap.values().stream()
                .filter(entry -> entry.getSongId().toGroupKey().equals(groupKey))
                .toList();
    }
}
