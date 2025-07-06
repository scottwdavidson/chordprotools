package com.pourchoices.chordpro.application.domain.port.out;

import com.pourchoices.chordpro.adapter.out.file.CatalogEntryDto;

import java.nio.file.Path;
import java.util.List;

public interface CatalogPort {

    List<CatalogEntryDto> readCatalogFromCsv(Path path);

    void writeCatalogToCsv(Path path, List<CatalogEntryDto> catalogEntryDtos);

}
