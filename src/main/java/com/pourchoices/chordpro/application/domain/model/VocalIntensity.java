package com.pourchoices.chordpro.application.domain.model;

/**
 * How much lead vocal a song requires — the venue-fit constraint from
 * {@code song-characterization-and-venue-fit.md}.
 *
 * <p>Deliberately kept separate from {@code energyLevel}: a mellow song can
 * still demand full lead vocal, and a rock song's energy doesn't imply
 * anything about whether it's vocal-heavy. A restaurant gig cares about this
 * axis specifically ("no vocals"), independent of how upbeat the song is.
 *
 * <p>Declared in ascending order ({@link #NONE} &lt; {@link #LIGHT} &lt;
 * {@link #FULL}) so venue policy can do a simple ordinal comparison:
 * {@code song.getVocalIntensity().ordinal() <= venue.getMaxVocalIntensity().ordinal()}.
 */
public enum VocalIntensity {

    /** Instrumental-friendly; no lead vocal required. */
    NONE,
    /** Some/occasional or quiet vocals. */
    LIGHT,
    /** Full lead vocal throughout. */
    FULL;

    /**
     * Parses a vocal-intensity string from the catalog CSV.
     *
     * @return {@code null} for null/blank/unrecognised values — absence means
     *     "not yet characterized," not "no vocals." Callers must not assume
     *     {@code null} means {@link #NONE}.
     */
    public static VocalIntensity fromString(String value) {
        if (value == null || value.isBlank()) return null;
        return switch (value.trim().toUpperCase()) {
            case "NONE"  -> NONE;
            case "LIGHT" -> LIGHT;
            case "FULL"  -> FULL;
            default      -> null;
        };
    }
}
