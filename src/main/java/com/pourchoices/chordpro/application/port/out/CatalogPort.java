package com.pourchoices.chordpro.application.port.out;

import com.pourchoices.chordpro.application.domain.model.CatalogEntry;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public interface CatalogPort {

    /**
     * @return the catalog keyed by SONG ID, in file order. The returned map
     *     is unmodifiable -- callers that need to mutate entries must copy it
     *     first (e.g. {@code new LinkedHashMap<>(result)} to preserve order).
     */
    Map<String, CatalogEntry> readCatalogFromCsv(Path path);

    void writeCatalogToCsv(Path path, List<CatalogEntry> catalogEntries);

}
