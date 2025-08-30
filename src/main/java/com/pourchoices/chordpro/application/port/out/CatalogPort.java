package com.pourchoices.chordpro.application.port.out;

import com.pourchoices.chordpro.adapter.out.file.CatalogEntryDto;
import com.pourchoices.chordpro.application.domain.model.CatalogEntry;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public interface CatalogPort {

    Map<String, CatalogEntry> readCatalogFromCsv(Path path);

    void writeCatalogToCsv(Path path, List<CatalogEntryDto> catalogEntryDtos);

}
