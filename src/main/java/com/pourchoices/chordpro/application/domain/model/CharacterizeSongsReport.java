package com.pourchoices.chordpro.application.domain.model;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;
import java.util.Map;

/**
 * Immutable result of a {@code characterize-songs} run.
 *
 * <p>Reports what was applied, what was skipped because a field was
 * already set (never silently overwritten), what ratings rows didn't
 * match any catalog group (likely a typo in the ratings file), and what
 * catalog groups still have no {@code energyLevel} after this run — the
 * "report what's still unrated, for manual review" requirement from
 * {@code song-characterization-and-venue-fit.md} \u00a76.
 */
@Value
@Builder
public class CharacterizeSongsReport {

    public enum RatingField {
        ENERGY_LEVEL, VOCAL_INTENSITY, GENRE_PRIMARY, GENRE_SECONDARY, SING_ALONG
    }

    @Value
    @Builder
    public static class FieldTally {
        int applied;
        int alreadySetSkipped;
    }

    int totalCatalogGroups;
    int ratingRowsProvided;
    int ratingRowsMatched;

    /** Ratings rows whose group key matched no catalog group — likely a typo. */
    @Singular
    List<String> unmatchedRatingGroupKeys;

    /** "Title — Artist" for every catalog group with no {@code energyLevel} after this run. */
    @Singular
    List<String> stillUnratedGroups;

    Map<RatingField, FieldTally> tallies;

    public int totalApplied() {
        return tallies.values().stream().mapToInt(FieldTally::getApplied).sum();
    }
}
