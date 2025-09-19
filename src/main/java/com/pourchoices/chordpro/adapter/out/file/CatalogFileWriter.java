package com.pourchoices.chordpro.adapter.out.file;

import com.opencsv.bean.HeaderColumnNameMappingStrategy;
import com.opencsv.bean.HeaderColumnNameMappingStrategyBuilder;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.pourchoices.chordpro.application.domain.service.HeaderFixer;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.util.List;

/**
 * Writer which writhes the entire ChordPro catalog
 */
@Service
public class CatalogFileWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogFileWriter.class);

    @SneakyThrows
    public void writeCatalogToCsv(Path path, List<CatalogEntryDto> catalogEntryDtos)  {

        LOGGER.info("Catalog File Path: {}", path.toFile().getAbsolutePath());

        // Create the writer
        Writer writer = new FileWriter(path.toFile());

        // Create a mapping strategy (assuming HeaderColumnNameMappingStrategy here, but use yours)
        HeaderColumnNameMappingStrategy<CatalogEntryDto> strategy = new HeaderColumnNameMappingStrategyBuilder<CatalogEntryDto>()
                .withType(CatalogEntryDto.class)
                .build();
        strategy.setColumnOrderOnWrite(new CustomColumnComparator(CatalogEntryDto.CATALOG_COLUMN_ORDER));

        // Use the builder to configure and build the writer
        StatefulBeanToCsv<CatalogEntryDto> beanToCsv = new StatefulBeanToCsvBuilder<CatalogEntryDto>(writer)
                .withApplyQuotesToAll(false) // This is important to prevent quotes around the empty fields
                .withMappingStrategy(strategy)
                .withThrowExceptions(false) // This is the key part to handle nulls gracefully
                .build();

        beanToCsv.write(catalogEntryDtos);
        writer.close();

    }

}
