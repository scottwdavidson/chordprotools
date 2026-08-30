package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import com.pourchoices.chordpro.application.domain.model.ChordProPath;
import com.pourchoices.chordpro.application.domain.model.SemanticDiffReport;
import com.pourchoices.chordpro.application.domain.model.SongId;
import com.pourchoices.chordpro.application.port.in.ConsistentSongDataUseCase;
import com.pourchoices.chordpro.application.port.in.VerifySyncUseCase;
import com.pourchoices.chordpro.application.port.out.CatalogPort;
import com.pourchoices.chordpro.config.ChordproCatalogIndexPathConfig;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Catalog-aware wrapper around {@link VerifySyncUseCase}'s drift engine —
 * resurrects the parked {@code consistent-song-data.md} design, but as a
 * thin layer over Phase 2's {@code SemanticDiffService} instead of a second
 * diff implementation (design doc §8).
 *
 * <p>Depends on the {@link VerifySyncUseCase} <em>port</em> rather than the
 * concrete {@code SemanticDiffService} — Spring wires whichever
 * implementation is registered, and this class never needs to know it's
 * {@code SemanticDiffService} on the other end.
 */
@Service
@AllArgsConstructor(onConstructor_ = @__(@Autowired))
@Slf4j
public class ConsistentSongDataService implements ConsistentSongDataUseCase {

    private final CatalogPort catalogPort;
    private final ChordproCatalogIndexPathConfig catalogConfig;
    private final VerifySyncUseCase verifySyncUseCase;

    @Override
    public List<SemanticDiffReport> check(SongId songId) {
        Path catalogPath = Paths.get(catalogConfig.getCatalogIndexPath());
        Map<String, CatalogEntry> catalog = catalogPort.readCatalogFromCsv(catalogPath);

        String groupKey = songId.toGroupKey();
        List<CatalogEntry> variants = catalog.values().stream()
                .filter(entry -> entry.getSongId().toGroupKey().equals(groupKey))
                .toList();

        if (variants.isEmpty()) {
            throw new IllegalArgumentException(
                    "No catalog entries found for song group '" + groupKey
                            + "' (from song ID: " + songId + ")");
        }

        if (variants.size() < 2) {
            log.info("consistent-song-data: '{}' has no key-variants to compare "
                    + "- nothing to check.", groupKey);
            return List.of();
        }

        // Reference: the base (standard-key) variant, or the first entry for
        // an orphan group with no base — same fallback ConsistentMetadataService uses.
        CatalogEntry reference = variants.stream()
                .filter(v -> v.getSongId().isBaseVersion())
                .findFirst()
                .orElse(variants.get(0));

        String referencePath = ChordProPath.toFilePath(reference.getSongId());

        List<SemanticDiffReport> reports = new ArrayList<>();
        for (CatalogEntry variant : variants) {
            if (variant == reference) {
                continue;
            }
            String variantPath = ChordProPath.toFilePath(variant.getSongId());
            reports.add(verifySyncUseCase.verifySync(referencePath, variantPath));
        }
        return reports;
    }
}
