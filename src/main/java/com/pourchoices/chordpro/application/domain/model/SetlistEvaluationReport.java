package com.pourchoices.chordpro.application.domain.model;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

/**
 * Immutable result of an {@code evaluate-setlist} run. Mirrors the shape of
 * {@link MetadataConsistencyReport} — structured findings plus summary
 * counters, formatting left entirely to the CLI command.
 *
 * <p>Everything here is designed to degrade gracefully when the catalog is
 * mostly uncharacterized (the state today, before Phase 3's bulk backfill
 * runs): missing data produces {@code uncharacterizedXxxCount} and
 * informational {@link #arcNotes}, never an exception.
 */
@Value
@Builder
public class SetlistEvaluationReport {

    /** Which venue-policy axis a {@link Violation} broke. */
    public enum ViolationType {
        VOCAL_INTENSITY, ENERGY_CEILING, ENERGY_FLOOR
    }

    /** One hard-policy violation for a single setlist song. */
    @Value
    @Builder
    public static class Violation {
        ViolationType type;
        String set;
        String title;
        String artist;
        /** Human-readable detail, already formatted. */
        String detail;
    }

    /** Resolved gig slug (may be null only if there were no assignments at all). */
    String gig;

    /** Venue name from {@code gig-venues.csv}. Null = no venue linked to this gig. */
    String venue;

    /** Policy from {@code venue-profiles.csv}. Null = venue unresolved, or no policy configured for it. */
    VenueProfile venueProfile;

    int totalSongs;
    int uncharacterizedEnergyCount;
    int uncharacterizedVocalIntensityCount;
    int uncharacterizedSingAlongCount;

    @Singular
    List<Violation> violations;

    @Singular("arcThird")
    List<ArcThird> arcThirds;

    /** Informational commentary about the arc / missing-policy state — never a hard failure. */
    @Singular("arcNote")
    List<String> arcNotes;

    int singAlongCount;
    int singAlongInFinalThird;

    /** Whether at least one of the last few songs (see scorer) is a sing-along. */
    boolean lastSongsHaveSingAlong;

    public int violationCount() {
        return violations.size();
    }
}
