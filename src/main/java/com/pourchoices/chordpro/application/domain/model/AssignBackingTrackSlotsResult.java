package com.pourchoices.chordpro.application.domain.model;

import lombok.Builder;
import lombok.Value;

/**
 * Structured outcome of {@link com.pourchoices.chordpro.application.port.in.AssignBackingTrackSlotsUseCase#assignSlots}.
 *
 * <p>In the default (preserve) mode, {@code preservedCount} reflects slots
 * that were already correct in {@code gigs.csv} - whether set by a previous
 * algorithm run or typed in by hand - and were left completely untouched.
 * {@code newlyAssignedCount} reflects only the blank rows that were filled.
 *
 * <p>In {@code --reoptimize} mode, every RC-backed song is recomputed from
 * scratch, so {@code preservedCount} is always {@code 0}.
 */
@Value
@Builder
public class AssignBackingTrackSlotsResult {

    Setlist setlist;
    int preservedCount;
    int newlyAssignedCount;
    boolean reoptimized;

    public int totalSlotted() {
        return preservedCount + newlyAssignedCount;
    }
}
