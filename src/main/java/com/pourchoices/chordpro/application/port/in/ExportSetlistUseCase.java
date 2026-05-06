package com.pourchoices.chordpro.application.port.in;

import com.pourchoices.chordpro.application.domain.model.Setlist;

public interface ExportSetlistUseCase {

    /**
     * Build the {@link Setlist} from the catalog and write it to {@code outputPathString}.
     *
     * @param outputPathString path where the setlist CSV should be written
     * @return the in-memory {@link Setlist} that was exported (useful for callers
     *         that want to do further processing without re-reading the file)
     */
    Setlist exportSetlist(String outputPathString);
}
