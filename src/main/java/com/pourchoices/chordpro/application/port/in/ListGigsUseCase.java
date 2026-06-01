package com.pourchoices.chordpro.application.port.in;

import com.pourchoices.chordpro.application.domain.model.GigSummary;

import java.util.List;

/**
 * Input port for {@code list-gigs}: list every distinct gig in {@code gigs.csv}
 * with a count of the songs assigned to it.
 *
 * @see com.pourchoices.chordpro.application.domain.service.ListGigsService
 */
public interface ListGigsUseCase {

    /**
     * Returns one {@link GigSummary} per distinct gig, sorted by gig slug
     * (date-first slugs sort chronologically).
     *
     * @return gig summaries; empty if there are no assignments
     */
    List<GigSummary> listGigs();
}
