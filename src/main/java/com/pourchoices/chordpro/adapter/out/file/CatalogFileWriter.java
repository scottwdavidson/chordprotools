package com.pourchoices.chordpro.adapter.out.file;

import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Reader which reads the entire ChordPro file into a list of "lines" to then be processed
 */
@Component
public class CatalogFileWriter {

    @SneakyThrows
    public void writeCatalogToCsv(Path path, List<CatalogEntryDto> catalogEntryDtos)  {
        try (Writer writer = Files.newBufferedWriter(path)) {
            StatefulBeanToCsv<CatalogEntryDto> beanToCsv = new StatefulBeanToCsvBuilder<CatalogEntryDto>(writer).build();
            beanToCsv.write(catalogEntryDtos);
        }
    }

}
