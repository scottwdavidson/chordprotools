package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.adapter.out.file.CatalogEntryDto;
import com.pourchoices.chordpro.adapter.out.file.CatalogFileWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;

@Component
public class GenerateIndexService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateIndexService.class);

    private final CatalogFileWriter catalogFileWriter;

    public GenerateIndexService(CatalogFileWriter catalogFileWriter) {
        this.catalogFileWriter = catalogFileWriter;
    }
    public void generateIndex(String filePathString){

        LOGGER.info("Entering generateIndex (service): {}", filePathString);

        Path path = Path.of(filePathString);

        List<CatalogEntryDto> catalogEntryDtos = List.of(
                CatalogEntryDto.builder()
                        .title("My Life")
                        .artist("Billy Joel")
                        .key("D")
                        .duration("3:45")
                        .backing("22")
                        .build(),
                CatalogEntryDto.builder()
                        .title("Sand In My Boots")
                        .artist("Morgan Wallen")
                        .key("C")
                        .duration("3:15")
                        .build());

        this.catalogFileWriter.writeCatalogToCsv(path,catalogEntryDtos);
    }
}
