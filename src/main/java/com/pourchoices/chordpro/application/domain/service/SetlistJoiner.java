package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import com.pourchoices.chordpro.application.domain.model.SetlistAssignment;
import com.pourchoices.chordpro.application.domain.model.SetlistEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Resolves a target gig and joins {@link SetlistAssignment} rows against the
 * catalog map to produce a list of {@link SetlistEntry} objects ready for
 * de-duplication and sorting.
 *
 * <p>Gig resolution (applied when the caller passes {@code null} or blank):
 * <ol>
 *   <li>Use the explicitly supplied {@code gigParam} if non-blank.</li>
 *   <li>Otherwise, fall back to the lexicographically <em>last</em> GIG value
 *       found in all assignments (date-first slugs sort chronologically).</li>
 *   <li>If there are no assignments at all, return an empty list and log a warning.</li>
 * </ol>
 *
 * <p>Songs referenced in an assignment but absent from the catalog are a
 * data-integrity error: the base version of every assigned song must exist.
 * An {@link IllegalStateException} is thrown if a catalog lookup fails.
 */
@Component
@Slf4j
public class SetlistJoiner {

    /**
     * Resolves the working gig from {@code gigParam} (or falls back to latest),
     * filters assignments to that gig, joins with the catalog, and returns
     * the resulting {@link SetlistEntry} list.
     *
     * @param gigParam      explicit gig slug from the CLI, or {@code null} / blank to auto-resolve
     * @param allAssignments all rows from {@code setlist-assignments.csv}
     * @param catalog        the full song catalog keyed by SONG ID string
     * @return joined entries for the resolved gig (may be empty)
     */
    public List<SetlistEntry> join(String gigParam,
                                   List<SetlistAssignment> allAssignments,
                                   Map<String, CatalogEntry> catalog) {

        String gig = resolveGig(gigParam, allAssignments);
        if (gig == null) {
            log.warn("No setlist assignments found — returning empty setlist.");
            return List.of();
        }
        log.info("Building setlist for gig: {}", gig);

        List<SetlistAssignment> gigAssignments = allAssignments.stream()
                .filter(a -> gig.equals(a.getGig()))
                .toList();
        log.info("Found {} assignment(s) for gig '{}'", gigAssignments.size(), gig);

        return gigAssignments.stream()
                .map(a -> buildEntry(a, catalog))
                .toList();
    }

    /**
     * Returns the gig slug to use: {@code gigParam} when non-blank, otherwise the
     * lexicographically last GIG value across all assignments.
     */
    public String resolveGig(String gigParam, List<SetlistAssignment> allAssignments) {
        if (gigParam != null && !gigParam.isBlank()) {
            return gigParam;
        }
        return allAssignments.stream()
                .map(SetlistAssignment::getGig)
                .max(String::compareTo)
                .orElse(null);
    }

    private SetlistEntry buildEntry(SetlistAssignment assignment,
                                    Map<String, CatalogEntry> catalog) {
        String id = assignment.getSongId().toString();
        CatalogEntry song = catalog.get(id);
        if (song == null) {
            throw new IllegalStateException(
                    "Setlist assignment references SONG ID '" + id
                    + "' which is not present in song-catalog.csv. "
                    + "The base version of every assigned song must exist in the catalog.");
        }
        return SetlistEntry.builder().song(song).assignment(assignment).build();
    }
}
