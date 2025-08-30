package com.pourchoices.chordpro.adapter.out.file;

import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import com.pourchoices.chordpro.application.port.out.CatalogPort;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Service
public class CatalogAdapter implements CatalogPort {

    private final CatalogFileReader catalogFileReader;
    private final CatalogFileWriter catalogFileWriter;

    public CatalogAdapter(CatalogFileReader catalogFileReader, CatalogFileWriter catalogFileWriter) {
        this.catalogFileReader = catalogFileReader;
        this.catalogFileWriter = catalogFileWriter;
    }

    @Override
    public Map<String, CatalogEntry> readCatalogFromCsv(Path catalogIndexPath) {
        return this.catalogFileReader.readCatalogFromCsv(catalogIndexPath);
    }

    @Override
    public void writeCatalogToCsv(Path path, List<CatalogEntryDto> catalogEntryDtos) {
        this.catalogFileWriter.writeCatalogToCsv(path,catalogEntryDtos);
    }
}
