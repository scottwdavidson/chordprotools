package com.pourchoices.chordpro.application.domain.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * One row of externally-supplied song characterization data, consumed by
 * {@code characterize-songs} to bulk-fill {@link CatalogEntry}'s
 * energy/vocal/genre/sing-along fields.
 *
 * <p>Keyed by {@link SongId#toGroupKey()} rather than title/artist text —
 * unambiguous, and a rating applies to every key-variant in the group at
 * once (energy/vocal/genre don't change with transposed key).
 *
 * <p>{@code title}/{@code artist} are carried along purely as
 * human-readable context for whoever authored the ratings file (so it's
 * reviewable without cross-referencing {@code song-catalog.csv}) — they are
 * never used for matching and are ignored if they drift from the catalog.
 */
@Value
@Builder
public class SongRating {

    /** Matches {@link SongId#toGroupKey()} for every variant this rating applies to. */
    @NonNull String groupKey;

    /** Reference only — not used for matching. */
    String title;
    /** Reference only — not used for matching. */
    String artist;

    /** Null = this rating doesn't set energy (rare; most rows should set it). */
    Integer energyLevel;
    VocalIntensity vocalIntensity;
    String genrePrimary;
    String genreSecondary;
    Boolean singAlong;
}
