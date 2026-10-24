package com.pourchoices.chordpro.application.domain.model;

/**
 * Which guitar (if any) the guitarist needs for a song — captured because
 * physically swapping instruments (acoustic &lt;-&gt; electric &lt;-&gt; bass) takes real
 * time mid-gig and disrupts the flow if it isn't planned for when ordering a
 * setlist.
 *
 * <p>Same "catalog-only characterization" spirit as {@link VocalIntensity}:
 * optional, backfilled over time, never required.
 */
public enum GuitarType {

    ACOUSTIC,
    ELECTRIC,
    BASS,
    /** Deliberately marked as not settled on one instrument (e.g. varies by arrangement). */
    UNKNOWN;

    /**
     * Parses a guitar-type string from the catalog CSV.
     *
     * @return {@code null} for null/blank/unrecognised values — absence means
     *     "not yet characterized," which is distinct from the explicit
     *     {@link #UNKNOWN} value. Callers must not treat {@code null} as
     *     {@link #UNKNOWN}.
     */
    public static GuitarType fromString(String value) {
        if (value == null || value.isBlank()) return null;
        return switch (value.trim().toUpperCase()) {
            case "ACOUSTIC" -> ACOUSTIC;
            case "ELECTRIC" -> ELECTRIC;
            case "BASS"     -> BASS;
            case "UNKNOWN"  -> UNKNOWN;
            default         -> null;
        };
    }
}
