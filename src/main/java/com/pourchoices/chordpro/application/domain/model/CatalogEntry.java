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
    String capo;
    String nord;
    String roland;
    String countin;
    /** Which device provides the backing track for this song; null = no backing. */
    BackingType backingType;
    String ve;
    String performanceKey;
    /** RC-500 display label — max 12 characters (hardware constraint). */
    String songLabel;

}
