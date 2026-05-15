package com.pourchoices.chordpro.application.port.in;

import com.pourchoices.chordpro.application.domain.model.Setlist;

public interface ExportSetlistUseCase {

    /**
     * Build the {@link Setlist} for the given gig and write it to {@code outputPathString}.
     *
     * @param gigParam        gig slug (e.g. {@code 2026-06-14-rusty-nail}), or {@code null}
     *                        to auto-resolve to the lexicographically latest gig
     * @param outputPathString path where the setlist CSV should be written
     * @return the in-memory {@link Setlist} (useful for callers that want further processing
     *         without re-reading the file)
     */
    Setlist exportSetlist(String gigParam, String outputPathString);
}
