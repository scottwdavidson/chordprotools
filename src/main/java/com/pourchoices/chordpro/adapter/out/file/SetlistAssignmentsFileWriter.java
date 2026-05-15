package com.pourchoices.chordpro.adapter.out.file;

import com.opencsv.bean.HeaderColumnNameMappingStrategy;
import com.opencsv.bean.HeaderColumnNameMappingStrategyBuilder;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.pourchoices.chordpro.application.domain.model.SetlistAssignment;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.util.List;

/**
 * Writes a list of {@link SetlistAssignment} domain objects to {@code setlist-assignments.csv}.
 */
@Service
@Slf4j
public class SetlistAssignmentsFileWriter {

    @SneakyThrows
    public void writeAssignments(Path path, List<SetlistAssignmentDto> dtos) {
        log.info("Writing {} setlist assignment(s) to {}", dtos.size(), path);

        Writer writer = new FileWriter(path.toFile());

        HeaderColumnNameMappingStrategy<SetlistAssignmentDto> strategy =
                new HeaderColumnNameMappingStrategyBuilder<SetlistAssignmentDto>()
                        .withType(SetlistAssignmentDto.class)
                        .build();
        strategy.setColumnOrderOnWrite(
                new CustomColumnComparator(SetlistAssignmentDto.COLUMN_ORDER));

        StatefulBeanToCsv<SetlistAssignmentDto> beanToCsv =
                new StatefulBeanToCsvBuilder<SetlistAssignmentDto>(writer)
                        .withApplyQuotesToAll(false)
                        .withMappingStrategy(strategy)
                        .withThrowExceptions(false)
                        .build();

        beanToCsv.write(dtos);
        writer.close();
    }
}
