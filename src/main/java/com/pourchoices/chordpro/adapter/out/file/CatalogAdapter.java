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
    private final CatalogEntryMapper catalogEntryMapper;

    public CatalogAdapter(CatalogFileReader catalogFileReader,
                          CatalogFileWriter catalogFileWriter,
                          CatalogEntryMapper catalogEntryMapper) {
        this.catalogFileReader = catalogFileReader;
        this.catalogFileWriter = catalogFileWriter;
        this.catalogEntryMapper = catalogEntryMapper;
    }

    @Override
    public Map<String, CatalogEntry> readCatalogFromCsv(Path catalogIndexPath) {
        return this.catalogFileReader.readCatalogFromCsv(catalogIndexPath);
    }

    @Override
    public void writeCatalogToCsv(Path path, List<CatalogEntry> catalogEntries) {
        List<CatalogEntryDto> dtos = this.catalogEntryMapper.toDtoList(catalogEntries);
        this.catalogFileWriter.writeCatalogToCsv(path, dtos);
    }
}
