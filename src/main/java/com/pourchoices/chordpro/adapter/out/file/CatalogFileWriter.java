package com.pourchoices.chordpro.adapter.out.file;

import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Writer which writhes the entire ChordPro catalog
 */
@Service
public class CatalogFileWriter {

    @SneakyThrows
    public void writeCatalogToCsv(Path path, List<CatalogEntryDto> catalogEntryDtos)  {
        try (Writer writer = Files.newBufferedWriter(path)) {
            StatefulBeanToCsv<CatalogEntryDto> beanToCsv = new StatefulBeanToCsvBuilder<CatalogEntryDto>(writer).build();
            beanToCsv.write(catalogEntryDtos);
        }
    }

}
