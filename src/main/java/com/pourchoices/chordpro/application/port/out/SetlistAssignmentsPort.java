package com.pourchoices.chordpro.application.port.out;

import com.pourchoices.chordpro.application.domain.model.SetlistAssignment;

import java.nio.file.Path;
import java.util.List;

/**
 * Output port for reading and writing {@code setlist-assignments.csv}.
 *
 * <p>{@link #readAssignments} always returns all rows. Filtering by gig
 * is the responsibility of the calling service.
 */
public interface SetlistAssignmentsPort {

    List<SetlistAssignment> readAssignments(Path path);

    void writeAssignments(Path path, List<SetlistAssignment> assignments);
}
