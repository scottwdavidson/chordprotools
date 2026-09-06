package com.pourchoices.chordpro.application.domain.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * Venue policy for a single gig — the constraint side of
 * {@code song-characterization-and-venue-fit.md}.
 *
 * <p>One row in {@code venue-profiles.csv}, hand-edited the same way as
 * {@code song-catalog.csv} and {@code gigs.csv} (Excel/Sheets, then
 * checked in). Deliberately kept as a separate file rather than columns on
 * {@code gigs.csv}: a gig's policy is a single constant value, but
 * {@code gigs.csv} has ~150 rows per gig (one per song) — repeating a
 * gig-constant value on every row would just be duplication for no benefit.
 *
 * <p>A gig with no matching row here has <b>no configured policy</b> — that
 * is a valid, common state (most gigs won't have one until Phase 2's
 * {@code evaluate-setlist} actually consumes this), not an error. Callers
 * must not assume a missing profile means "no constraints" or "strictest
 * constraints" — it means "not yet configured," and should say so.
 */
@Value
@Builder(toBuilder = true)
public class VenueProfile {

    /** Gig identifier — same slug used in {@code gigs.csv}, e.g. {@code 2026-06-14-rusty-nail}. */
    @NonNull String gig;

    /**
     * Loudest vocal treatment this venue tolerates. Null = not configured for
     * this gig (not the same as {@link VocalIntensity#NONE}).
     */
    VocalIntensity maxVocalIntensity;

    /** Highest {@code energyLevel} (1-10) this venue tolerates. Null = no ceiling configured. */
    Integer energyCeiling;

    /** Lowest {@code energyLevel} (1-10) this venue tolerates. Null = no floor configured. */
    Integer energyFloor;
}
