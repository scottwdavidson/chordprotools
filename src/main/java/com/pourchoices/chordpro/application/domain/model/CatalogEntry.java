package com.pourchoices.chordpro.application.domain.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Builder(toBuilder = true)
@Value
public class CatalogEntry {

    @NonNull
    SongId songId;
    @NonNull
    String title;
    @NonNull
    String artist;
    @NonNull
    String key;
    @NonNull
    String duration;
    String tempo;
    String timeSignature;
    String nord;
    String roland;
    String countin;
    /** Which device provides the backing track for this song; null = no backing. */
    BackingType backingType;
    String ve;
    String performanceKey;
    /** RC-500 display label — max 12 characters (hardware constraint). */
    String songLabel;

    // --- Setlist/venue-fit characterization (song-characterization-and-venue-fit.md) ---
    // Deliberately catalog-only for now: NOT echoed into .cho file headers by
    // CatalogEntryToParsedHeaderMapper (unlike tempo/key/countin, these aren't
    // things a musician needs visible on the chart mid-song). Revisit if that
    // changes.

    /** Mellow ballad (1) to dance/upbeat (10). Null = not yet characterized. */
    Integer energyLevel;
    /** How much lead vocal this song requires. Null = not yet characterized. */
    VocalIntensity vocalIntensity;
    /** Descriptive only — never used to gate/require setlist placement. */
    String genrePrimary;
    /** Optional. Descriptive only, same as {@link #genrePrimary}. */
    String genreSecondary;
    /** Crowd-engagement marker for the arc scorer. Null = not yet characterized. */
    Boolean singAlong;

}
