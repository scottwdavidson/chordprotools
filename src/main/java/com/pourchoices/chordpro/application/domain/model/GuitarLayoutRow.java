package com.pourchoices.chordpro.application.domain.model;

import lombok.Builder;
import lombok.Value;

/**
 * One song's line in the full guitar layout of a setlist — unlike
 * {@link GuitarSwitch} (which only exists where the instrument changes),
 * every performed song gets a row, so the whole guitar plan can be read
 * top to bottom on one page.
 */
@Value
@Builder
public class GuitarLayoutRow {

    /** Set position, e.g. {@code A05}. */
    String set;

    String title;
    String artist;

    /** Null = not yet characterized (distinct from the explicit {@link GuitarType#UNKNOWN}). */
    GuitarType guitar;

    /** True when this song needs a different guitar than the previous one (same rule as {@link GuitarSwitch}). */
    boolean switchFromPrevious;
}
