package com.pourchoices.chordpro.application.domain.model;

import lombok.Builder;
import lombok.Value;

/**
 * Energy summary for one third of an ordered setlist — the unit produced by
 * {@code SetlistArcScorer} and consumed by {@code evaluate-setlist}.
 *
 * <p>{@code avgEnergy}/{@code medianEnergy} are computed only over songs that
 * have an {@code energyLevel} set — {@code characterizedCount} tells the
 * caller how much of {@code songCount} that average is actually based on.
 * Both are {@code null} when no song in this third has been characterized
 * yet (the common state today, since bulk backfill hasn't run).
 */
@Value
@Builder
public class ArcThird {

    /** e.g. {@code "Opening"}, {@code "Middle"}, {@code "Final"}. */
    String label;

    /** Total songs in this third, characterized or not. */
    int songCount;

    /** How many of {@link #songCount} have a non-null {@code energyLevel}. */
    int characterizedCount;

    /** Null if {@link #characterizedCount} is zero. */
    Double avgEnergy;

    /** Null if {@link #characterizedCount} is zero. */
    Double medianEnergy;
}
