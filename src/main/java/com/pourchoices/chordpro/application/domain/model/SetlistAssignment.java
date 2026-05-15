package com.pourchoices.chordpro.application.domain.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * Represents a single song's assignment to a specific gig setlist.
 *
 * <p>One row in {@code setlist-assignments.csv}.  The same song may appear in
 * many gigs — each occurrence is a distinct {@code SetlistAssignment}.
 *
 * <p>The {@code set} field uses the compound position code (e.g. {@code A01},
 * {@code B03}, {@code Z1}) that encodes both the set letter and the song's
 * position within that set.
 */
@Value
@Builder
public class SetlistAssignment {

    /** Gig identifier — date-first slug, e.g. {@code 2026-06-14-rusty-nail}. */
    @NonNull String gig;

    /** Foreign key into {@code song-catalog.csv}. */
    @NonNull SongId songId;

    /** Compound set-position code, e.g. {@code A01}. */
    @NonNull String set;
}
