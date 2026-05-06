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
import java.util.Comparator;
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

        // 2. Filter to entries that belong to a set and sort by set code
        List<CatalogEntry> setlistEntries = catalogMap.values().stream()
                .filter(entry -> entry.getSet() != null && !entry.getSet().isBlank())
                .sorted(Comparator.comparing(CatalogEntry::getSet))
                .toList();

        log.info("Found {} entries with a Set value", setlistEntries.size());

        // 3. Wrap in the Setlist domain object
        Setlist setlist = Setlist.builder()
                .entries(setlistEntries)
                .build();

        // 4. Write the setlist CSV — the adapter owns the DTO projection
        Path outputPath = Paths.get(outputPathString);
        setlistPort.writeSetlistToCsv(outputPath, setlistEntries);
        log.info("Setlist ({} songs) written to {}", setlist.size(), outputPath.toAbsolutePath());

        return setlist;
    }
}
