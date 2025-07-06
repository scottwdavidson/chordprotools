package com.pourchoices.chordpro.adapter.out.file;

import com.pourchoices.chordpro.application.domain.port.out.CatalogPort;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;

@Service
public class CatalogAdapter implements CatalogPort {

    private CatalogFileReader catalogFileReader;
    private CatalogFileWriter catalogFileWriter;
    @Override
    public List<CatalogEntryDto> readCatalogFromCsv(Path path) {
        return this.catalogFileReader.readCatalogFromCsv(path);
    }

    @Override
    public void writeCatalogToCsv(Path path, List<CatalogEntryDto> catalogEntryDtos) {

    }
}
