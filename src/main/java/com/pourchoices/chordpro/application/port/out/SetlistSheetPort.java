package com.pourchoices.chordpro.application.port.out;

import com.pourchoices.chordpro.application.domain.model.SetlistEntry;

import java.nio.file.Path;
import java.util.List;

/** Outbound port for the stage-ready, print-friendly setlist sheet. */
public interface SetlistSheetPort {

    /**
     * Write a printable sheet with one printed page per element of {@code pages}.
     *
     * @param outputPath where the sheet should be written
     * @param gig        gig slug, shown as the sheet heading
     * @param pages      songs grouped by printed page, in order
     */
    void writeSetlistSheet(Path outputPath, String gig, List<List<SetlistEntry>> pages);
}
