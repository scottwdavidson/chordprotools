package com.pourchoices.chordpro.application.port.in;

import com.pourchoices.chordpro.application.domain.model.Setlist;

public interface ExportSetlistUseCase {

    /**
     * Build the {@link Setlist} for the given gig and write it to {@code outputPathString}.
     *
     * @param gigParam         gig slug (e.g. {@code 2026-06-14-rusty-nail}), or {@code null}
     *                         to auto-resolve to the lexicographically latest gig
     * @param outputPathString path where the setlist CSV should be written
     * @param includeBackup    when {@code true}, Z-set backup songs are included in both
     *                         the CSV output and the returned {@link Setlist};
     *                         when {@code false} (default) only the fan-facing sets are included
     * @return the in-memory {@link Setlist}
     */
    Setlist exportSetlist(String gigParam, String outputPathString, boolean includeBackup);
}
