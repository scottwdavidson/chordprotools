package com.pourchoices.chordpro.application.domain.model;

import lombok.Builder;
import lombok.Value;

/**
 * One guitar-instrument-swap point in an ordered setlist — the song at
 * {@link #getToTitle()} needs a different guitar than the one just played
 * for {@link #getFromTitle()}. Produced by {@code GuitarSwitchScorer} and
 * surfaced by {@code evaluate-setlist} so a switch can be planned for (or
 * avoided by reordering) instead of discovered live, mid-gig.
 */
@Value
@Builder
public class GuitarSwitch {

    /** Set position of the song being switched TO, e.g. {@code A05}. */
    String set;

    String fromTitle;
    String fromArtist;
    GuitarType fromGuitar;

    String toTitle;
    String toArtist;
    GuitarType toGuitar;
}
