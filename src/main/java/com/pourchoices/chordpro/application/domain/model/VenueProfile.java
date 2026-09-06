package com.pourchoices.chordpro.application.domain.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * Venue policy — the constraint side of {@code song-characterization-and-venue-fit.md}.
 *
 * <p>One row in {@code venue-profiles.csv}, hand-edited the same way as
 * {@code song-catalog.csv} and {@code gigs.csv} (Excel/Sheets, then checked
 * in). Keyed by <b>venue name</b>, not gig — a venue's policy is intrinsic
 * to the venue itself and doesn't change gig to gig. This also lets a venue
 * be characterized before it ever has a confirmed gig (e.g. to build a
 * candidate setlist to pitch to a prospective venue's proprietor), and
 * avoids re-stating the same policy on every gig a repeat venue hosts —
 * both of which a gig-keyed model got wrong in practice (Moods Wine Bar
 * needed the identical policy copy-pasted across 3 separate gig rows before
 * this refactor).
 *
 * <p>Which gig happened at which venue is a separate, thin association —
 * see {@code gig-venues.csv} / {@code GigVenuePort}. A gig with no entry
 * there simply has no venue attached yet (e.g. a not-yet-booked pitch gig).
 *
 * <p>A venue with no matching row here has <b>no configured policy</b> —
 * that is a valid, common state (most venues won't have one until Phase 2's
 * {@code evaluate-setlist} actually consumes this), not an error. Callers
 * must not assume a missing profile means "no constraints" or "strictest
 * constraints" — it means "not yet configured," and should say so.
 */
@Value
@Builder(toBuilder = true)
public class VenueProfile {

    /** Venue name — the natural key, e.g. {@code "Moods Wine Bar"}. */
    @NonNull String venue;

    /**
     * Loudest vocal treatment this venue tolerates. Null = not configured for
     * this venue (not the same as {@link VocalIntensity#NONE}).
     */
    VocalIntensity maxVocalIntensity;

    /** Highest {@code energyLevel} (1-10) this venue tolerates. Null = no ceiling configured. */
    Integer energyCeiling;

    /** Lowest {@code energyLevel} (1-10) this venue tolerates. Null = no floor configured. */
    Integer energyFloor;
}
