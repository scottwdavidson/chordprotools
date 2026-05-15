package com.pourchoices.chordpro.application.port.in;

import com.pourchoices.chordpro.application.domain.model.Setlist;

/**
 * Input port for the assign-backing-track-slots command.
 *
 * <p>Reassigns RC-500 backing-track slot numbers for all songs in the specified
 * gig's setlist, writes the updated values back into the individual ChordPro files
 * and the master song catalog, then regenerates the setlist CSV.
 */
public interface AssignBackingTrackSlotsUseCase {

    /**
     * @param gigParam   gig slug (e.g. {@code 2026-06-14-rusty-nail}), or {@code null}
     *                   to auto-resolve to the lexicographically latest gig
     * @param outputPath path for the regenerated setlist CSV
     * @return the updated {@link Setlist} (set-ordered, de-duplicated)
     */
    Setlist assignSlots(String gigParam, String outputPath);
}
