package com.pourchoices.chordpro.adapter.out.file;

import com.opencsv.bean.CsvToBeanBuilder;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Reader which reads the entire ChordPro file into a list of "lines" to then be processed
 */
public class CatalogFileReader {

    public static List<CatalogEntryDto> readCatalogFromCsv(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path)) {
            return new CsvToBeanBuilder<CatalogEntryDto>(reader)
                    .withType(CatalogEntryDto.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build()
                    .parse();
        }


    }
}
