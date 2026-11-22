package com.pourchoices.chordpro.adapter.out.file;

import com.pourchoices.chordpro.application.domain.model.SetlistEntry;
import com.pourchoices.chordpro.application.port.out.FanSetlistSheetPort;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;

/** Writes the fans' setlist handout as an HTML file, creating its folder if needed. */
@Service
@AllArgsConstructor
@Slf4j
public class FanSetlistSheetAdapter implements FanSetlistSheetPort {

    private final FanSetlistHtmlRenderer renderer;

    @Override
    public void writeFanSetlistSheet(Path outputPath, String gig, String venue, List<List<SetlistEntry>> sets) {
        log.info("Fan setlist file path: {}", outputPath.toAbsolutePath());
        Html.write(outputPath, renderer.render(gig, venue, sets));
    }
}
