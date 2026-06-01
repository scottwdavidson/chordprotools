package com.pourchoices.chordpro.application.domain.model;

import lombok.Builder;
import lombok.Value;

/**
 * A single search result from {@code find-song-id}: one row per song
 * (the base/standard-key version), annotated with how many key variants exist.
 *
 * <p>The {@link #displaySongId} is the value safe to paste into {@code gigs.csv}:
 * for a normal song it is the group key (base SONG ID); for an
 * {@link #orphan orphaned} variant (a key variant whose base version is missing
 * from the catalog) it is the variant's own ID, flagged so the user knows the
 * data needs fixing before use.
 */
@Value
@Builder
public class SongMatch {

    /** Song title of the representative (base, or promoted variant if orphan). */
    String title;

    /** Artist of the representative. */
    String artist;

    /** Musical key of the representative. */
    String key;

    /**
     * SONG ID to display / paste into a setlist.
     * Group key for a normal match; the variant's full ID for an orphan.
     */
    String displaySongId;

    /** Number of key-variant files that exist for this song (excludes the base). */
    int variantCount;

    /**
     * {@code true} when no base (standard-key) version exists in the catalog and
     * a key variant had to be promoted as the representative — a data-integrity
     * issue the user should resolve before using the ID in a setlist.
     */
    boolean orphan;
}
