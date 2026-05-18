package com.pourchoices.chordpro.adapter.out.file;

import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import com.pourchoices.chordpro.application.domain.model.SetlistAssignment;
import com.pourchoices.chordpro.application.port.out.SetlistAssignmentsPort;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Adapter implementing {@link SetlistAssignmentsPort} via CSV file I/O.
 *
 * <p>Delegates reading to {@link SetlistAssignmentsFileReader} and writing to
 * {@link SetlistAssignmentsFileWriter}, matching the pattern used by
 * {@link CatalogAdapter}.
 */
@Service
public class SetlistAssignmentsAdapter implements SetlistAssignmentsPort {

    private final SetlistAssignmentsFileReader reader;
    private final SetlistAssignmentsFileWriter writer;
    private final SetlistAssignmentMapper mapper;

    public SetlistAssignmentsAdapter(SetlistAssignmentsFileReader reader,
                                     SetlistAssignmentsFileWriter writer,
                                     SetlistAssignmentMapper mapper) {
        this.reader = reader;
        this.writer = writer;
        this.mapper = mapper;
    }

    @Override
    public List<SetlistAssignment> readAssignments(Path path) {
        return reader.readAssignments(path);
    }

    @Override
    public void writeAssignments(Path path, List<SetlistAssignment> assignments) {
        writer.writeAssignments(path, mapper.toDtoList(assignments));
    }

    @Override
    public void writeEnrichedAssignments(Path path,
                                         List<SetlistAssignment> assignments,
                                         Map<String, CatalogEntry> catalog) {
        writer.writeAssignments(path, mapper.toEnrichedDtoList(assignments, catalog));
    }
}
