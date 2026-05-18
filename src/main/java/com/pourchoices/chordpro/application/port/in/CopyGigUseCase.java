package com.pourchoices.chordpro.application.port.in;

/**
 * Creates a new gig by copying all setlist assignments from an existing source gig.
 *
 * <p>The resulting assignments are written with enriched TITLE and ARTIST columns
 * so the CSV is human-readable in Google Sheets without cross-referencing
 * {@code song-catalog.csv}.
 */
public interface CopyGigUseCase {

    /**
     * Copies all assignments from {@code sourceGig} to {@code targetGig}.
     *
     * @param sourceGig slug of the gig to copy from (must exist)
     * @param targetGig slug of the new gig (must not already have assignments,
     *                  unless {@code force} is {@code true})
     * @param force     when {@code true}, any existing assignments for
     *                  {@code targetGig} are replaced
     * @return number of assignments written for the new gig
     * @throws IllegalArgumentException if {@code sourceGig} is not found, or
     *                                  {@code targetGig} already has assignments
     *                                  and {@code force} is {@code false}
     */
    int copyGig(String sourceGig, String targetGig, boolean force);
}
