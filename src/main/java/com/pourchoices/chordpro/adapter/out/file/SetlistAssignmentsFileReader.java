package com.pourchoices.chordpro.adapter.out.file;

import com.opencsv.bean.CsvToBeanBuilder;
import com.pourchoices.chordpro.application.domain.model.SetlistAssignment;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Reads {@code setlist-assignments.csv} into a list of {@link SetlistAssignment} domain objects.
 */
@Service
@Slf4j
public class SetlistAssignmentsFileReader {

    private final SetlistAssignmentMapper mapper;

    SetlistAssignmentsFileReader(SetlistAssignmentMapper mapper) {
        this.mapper = mapper;
    }

    @SneakyThrows
    public List<SetlistAssignment> readAssignments(Path path) {
        log.info("Reading setlist assignments from {}", path);

        List<SetlistAssignmentDto> dtos;
        try (Reader reader = Files.newBufferedReader(path.toAbsolutePath())) {
            dtos = new CsvToBeanBuilder<SetlistAssignmentDto>(reader)
                    .withType(SetlistAssignmentDto.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build()
                    .parse();
        }

        List<SetlistAssignment> assignments = mapper.toEntityList(dtos);
        log.info("Read {} setlist assignment(s)", assignments.size());
        return assignments;
    }
}
