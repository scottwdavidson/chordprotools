package com.pourchoices.chordpro.adapter.out.file;

import com.opencsv.bean.CsvToBeanBuilder;
import com.pourchoices.chordpro.adapter.in.file.UpdateSongCommand;
import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Reader which reads the entire ChordPro file into a list of "lines" to then be processed
 */
@Service
public class CatalogFileReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogFileReader.class);

    private CatalogEntryMapper catalogEntryMapper;

    CatalogFileReader(CatalogEntryMapper catalogEntryMapper) {
        this.catalogEntryMapper = catalogEntryMapper;
    }

    @SneakyThrows
    public Map<String, CatalogEntry> readCatalogFromCsv(Path catalogIndexPath)  {

        LOGGER.info("catalogIndexPath: {}", catalogIndexPath);

        List<CatalogEntryDto> dtoCatalog;
        try (Reader reader = Files.newBufferedReader(Path.of(catalogIndexPath.toFile().getAbsolutePath()))) {
            dtoCatalog =  new CsvToBeanBuilder<CatalogEntryDto>(reader)
                    .withType(CatalogEntryDto.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build()
                    .parse();
        }

        // Map to domain object
        List<CatalogEntry> catalog = catalogEntryMapper.toEntityList(dtoCatalog);

        // Create an immutable map using Java Streams
        return catalog.stream()
                .collect(Collectors.toUnmodifiableMap(CatalogEntry::getChordProFilename, dto -> dto));

    }
}
