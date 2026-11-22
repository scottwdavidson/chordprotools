package com.pourchoices.chordpro.adapter.out.file;

import com.pourchoices.chordpro.application.domain.model.SetlistEntry;
import com.pourchoices.chordpro.application.port.out.SetlistSheetPort;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;

/** Writes the printable floor setlist sheet as an HTML file, creating its folder if needed. */
@Service
@AllArgsConstructor
@Slf4j
public class SetlistSheetAdapter implements SetlistSheetPort {

    private final SetlistSheetHtmlRenderer renderer;

    @Override
    public void writeSetlistSheet(Path outputPath, String gig, List<List<SetlistEntry>> pages) {
        log.info("Setlist sheet file path: {}", outputPath.toAbsolutePath());
        Html.write(outputPath, renderer.render(gig, pages));
    }
}
