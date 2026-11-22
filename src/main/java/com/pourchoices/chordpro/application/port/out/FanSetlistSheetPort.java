package com.pourchoices.chordpro.application.port.out;

import com.pourchoices.chordpro.application.domain.model.SetlistEntry;

import java.nio.file.Path;
import java.util.List;

/** Outbound port for the printable setlist handout given to fans. */
public interface FanSetlistSheetPort {

    /**
     * Write a printable handout listing every set's songs.
     *
     * @param outputPath where the handout should be written
     * @param gig        gig slug (its leading date, if any, is shown on the handout)
     * @param venue      venue name, or {@code null} when the gig has none linked
     * @param sets       songs grouped by set, in play order
     */
    void writeFanSetlistSheet(Path outputPath, String gig, String venue, List<List<SetlistEntry>> sets);
}
