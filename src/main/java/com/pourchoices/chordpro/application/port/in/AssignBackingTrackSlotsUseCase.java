package com.pourchoices.chordpro.application.port.in;

import com.pourchoices.chordpro.application.domain.model.AssignBackingTrackSlotsResult;

/**
 * Input port for the assign-backing-track-slots command.
 *
 * <p>Default behavior (reoptimize = false) <b>preserves</b> every non-blank
 * RC SLOT already in {@code gigs.csv} for the target gig - whether it was
 * set by a previous run of this algorithm or typed in by hand - and only
 * computes new slot numbers for songs that don't have one yet. Every
 * currently-assigned slot (preserved or newly computed) is then synced into
 * its {@code .cho} file's {@code {meta: rc-slot}} directive, annotated with
 * the gig name, skipping the write entirely when nothing actually changed.
 *
 * <p>{@code reoptimize = true} ignores whatever is already in {@code gigs.csv}
 * and fully recomputes every slot from scratch, in setlist order - the
 * original behavior, for when a genuine renumber is worth the RC-500 rework.
 */
public interface AssignBackingTrackSlotsUseCase {

    /**
     * @param gigParam   gig slug (e.g. {@code 2026-06-14-rusty-nail}), or {@code null}
     *                   to auto-resolve to the lexicographically latest gig
     * @param outputPath path for the regenerated setlist CSV
     * @param reoptimize {@code true} to fully recompute every slot from scratch;
     *                   {@code false} (default) to preserve existing values and
     *                   only fill in blanks
     * @return the structured result: updated {@link com.pourchoices.chordpro.application.domain.model.Setlist}
     *         plus preserved/newly-assigned counts
     * @throws IllegalArgumentException in preserve mode, if {@code gigs.csv} has a
     *         non-numeric or out-of-range RC SLOT, or two different songs claiming
     *         the same slot, for this gig - nothing is written in that case
     */
    AssignBackingTrackSlotsResult assignSlots(String gigParam, String outputPath, boolean reoptimize);
}
