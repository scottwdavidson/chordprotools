package com.pourchoices.chordpro.adapter.out.file;

import com.opencsv.bean.CsvToBeanBuilder;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Reader which reads the entire ChordPro file into a list of "lines" to then be processed
 */
@Service
public class CatalogFileReader {

    @SneakyThrows
    public List<CatalogEntryDto> readCatalogFromCsv(Path path)  {
        try (Reader reader = Files.newBufferedReader(path)) {
            return new CsvToBeanBuilder<CatalogEntryDto>(reader)
                    .withType(CatalogEntryDto.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build()
                    .parse();
        }


    }
}
