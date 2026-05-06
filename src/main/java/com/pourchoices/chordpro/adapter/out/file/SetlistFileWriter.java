package com.pourchoices.chordpro.adapter.out.file;

import com.opencsv.bean.HeaderColumnNameMappingStrategy;
import com.opencsv.bean.HeaderColumnNameMappingStrategyBuilder;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import java.io.FileWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.util.List;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Writes a setlist to a CSV file using the slim {@link SetlistEntryDto} view —
 * four columns only: SET, SONG TITLE, SONG ARTIST, KEY.
 *
 * <p>Intentionally separate from {@link CatalogFileWriter} so that changes to
 * the setlist output format never accidentally affect the master catalog writer.
 */
@Service
@Slf4j
public class SetlistFileWriter {

    @SneakyThrows
    public void writeSetlistToCsv(Path path, List<SetlistEntryDto> entries) {

        log.info("Setlist file path: {}", path.toFile().getAbsolutePath());

        Writer writer = new FileWriter(path.toFile());

        HeaderColumnNameMappingStrategy<SetlistEntryDto> strategy =
                new HeaderColumnNameMappingStrategyBuilder<SetlistEntryDto>()
                        .withType(SetlistEntryDto.class)
                        .build();
        strategy.setColumnOrderOnWrite(
                new CustomColumnComparator(SetlistEntryDto.SETLIST_COLUMN_ORDER));

        StatefulBeanToCsv<SetlistEntryDto> beanToCsv =
                new StatefulBeanToCsvBuilder<SetlistEntryDto>(writer)
                        .withApplyQuotesToAll(false)
                        .withMappingStrategy(strategy)
                        .withThrowExceptions(false)
                        .build();

        beanToCsv.write(entries);
        writer.close();
    }
}
