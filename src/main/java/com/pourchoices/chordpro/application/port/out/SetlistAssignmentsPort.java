package com.pourchoices.chordpro.application.port.out;

import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import com.pourchoices.chordpro.application.domain.model.SetlistAssignment;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Output port for reading and writing {@code setlist-assignments.csv}.
 *
 * <p>{@link #readAssignments} always returns all rows.  Filtering by gig
 * is the responsibility of the calling service.
 *
 * <p>{@link #writeEnrichedAssignments} should be preferred over the bare
 * {@link #writeAssignments} whenever the caller has catalog context — it
 * populates the decorative TITLE and ARTIST columns so the CSV is
 * human-readable when opened directly in Google Sheets or Excel.
 */
public interface SetlistAssignmentsPort {

    List<SetlistAssignment> readAssignments(Path path);

    /** Writes assignments with blank TITLE/ARTIST columns. */
    void writeAssignments(Path path, List<SetlistAssignment> assignments);

    /**
     * Writes assignments with TITLE and ARTIST populated from the catalog map,
     * making the CSV readable in Sheets without cross-referencing song-catalog.csv.
     * Songs absent from the catalog get blank title/artist values.
     */
    void writeEnrichedAssignments(Path path,
                                  List<SetlistAssignment> assignments,
                                  Map<String, CatalogEntry> catalog);
}
