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

    /**
     * Reads {@code gigs.csv} as raw text lines, with no CSV field-count
     * validation - used by {@code tidy-gigs} to detect and repair a
     * malformed file before attempting the strict {@link #readAssignments}
     * parse. Returns an empty list if the file doesn't exist.
     */
    List<String> readRawLines(Path path);

    /** Writes raw text lines back verbatim - the repair counterpart to {@link #readRawLines}. */
    void writeRawLines(Path path, List<String> lines);
}
