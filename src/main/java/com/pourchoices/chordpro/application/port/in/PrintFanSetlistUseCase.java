package com.pourchoices.chordpro.application.port.in;

import com.pourchoices.chordpro.application.domain.model.Setlist;

public interface PrintFanSetlistUseCase {

    /**
     * Build the played setlist (no Z-set backups) for the given gig and write it as a printable
     * handout for fans: the band, the venue and date, then each set's songs by title and artist.
     *
     * @param gigParam         gig slug, or {@code null} to auto-resolve to the latest gig
     * @param outputPathString where the printable handout should be written
     * @return the in-memory {@link Setlist} that was printed
     */
    Setlist printFanSetlist(String gigParam, String outputPathString);
}
