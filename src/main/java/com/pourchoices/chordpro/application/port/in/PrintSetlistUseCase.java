package com.pourchoices.chordpro.application.port.in;

import com.pourchoices.chordpro.application.domain.model.Setlist;

public interface PrintSetlistUseCase {

    /**
     * Build the fan-facing {@link Setlist} (no Z-set backups) for the given gig and write it
     * as a printable floor sheet: portrait pages, one {@code A01-45 Song Title} row per song.
     *
     * @param gigParam         gig slug, or {@code null} to auto-resolve to the latest gig
     * @param outputPathString where the printable sheet should be written
     * @return the in-memory {@link Setlist} that was printed
     */
    Setlist printSetlist(String gigParam, String outputPathString);
}
