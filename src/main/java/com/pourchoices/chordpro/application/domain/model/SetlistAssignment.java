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
 *
 * <p><strong>Invariant:</strong> {@code songId} must always be a base version
 * (i.e. {@link SongId#isBaseVersion()} returns {@code true}).  Key variants
 * (e.g. {@code MyLife-c}) must never be used as setlist foreign keys; only the
 * base song identity (e.g. {@code MyLife}) is valid.  Enforcement is the
 * responsibility of the mapper layer when reading from CSV.
 */
@Value
@Builder(toBuilder = true)
public class SetlistAssignment {

    /** Gig identifier — date-first slug, e.g. {@code 2026-06-14-rusty-nail}. */
    @NonNull String gig;

    /** Foreign key into {@code song-catalog.csv}. */
    @NonNull SongId songId;

    /** Compound set-position code, e.g. {@code A01}. */
    @NonNull String set;

    /**
     * RC-500 slot assigned for this specific gig, e.g. {@code "11"}.
     * Null / blank until {@code assign-backing-track-slots} has been run for this gig.
     * Never copied when a gig is cloned via {@code copy-gig}.
     */
    String rcSlot;
}
