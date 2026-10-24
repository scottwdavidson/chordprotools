package com.pourchoices.chordpro.adapter.out.file;

import com.opencsv.bean.CsvToBeanBuilder;
import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Reader which reads the entire ChordPro file into a list of "lines" to then be processed
 */
@Service
@Slf4j
public class CatalogFileReader {

    private CatalogEntryMapper catalogEntryMapper;

    CatalogFileReader(CatalogEntryMapper catalogEntryMapper) {
        this.catalogEntryMapper = catalogEntryMapper;
    }

    @SneakyThrows
    public Map<String, CatalogEntry> readCatalogFromCsv(Path catalogIndexPath)  {

        log.info("catalogIndexPath: {}", catalogIndexPath);

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

        // LinkedHashMap preserves file order — callers that read-modify-write the
        // whole catalog (characterize-songs, import-song, consistent-metadata --fix)
        // rely on this to avoid silently re-sorting song-catalog.csv into hash order
        // on every write. Collectors.toUnmodifiableMap gives NO ordering guarantee,
        // which is exactly the bug this replaced.
        Map<String, CatalogEntry> ordered = catalog.stream()
                .collect(Collectors.toMap(
                        e -> e.getSongId().toString(),
                        e -> e,
                        (a, b) -> { throw new IllegalStateException("Duplicate SONG ID in catalog: " + a.getSongId()); },
                        LinkedHashMap::new));
        return Collections.unmodifiableMap(ordered);

    }
}
