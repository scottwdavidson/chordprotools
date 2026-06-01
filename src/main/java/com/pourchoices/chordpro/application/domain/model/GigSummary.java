package com.pourchoices.chordpro.application.domain.model;

import lombok.Value;

/**
 * One row of {@code list-gigs} output: a gig slug and the number of songs
 * assigned to it in {@code gigs.csv}.
 */
@Value
public class GigSummary {

    /** Gig identifier — date-first slug, e.g. {@code 2026-06-14-rusty-nail}. */
    String gig;

    /** Number of song assignments for this gig. */
    int songCount;
}
