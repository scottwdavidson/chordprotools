package com.pourchoices.chordpro.adapter.out.file;

import com.pourchoices.chordpro.application.domain.model.SetlistEntry;
import com.pourchoices.chordpro.application.port.out.SetlistSheetPort;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Writes the printable setlist sheet as an HTML file. */
@Service
@AllArgsConstructor
@Slf4j
public class SetlistSheetAdapter implements SetlistSheetPort {

    private final SetlistSheetHtmlRenderer renderer;

    @Override
    @SneakyThrows
    public void writeSetlistSheet(Path outputPath, String gig, List<List<SetlistEntry>> pages) {
        log.info("Setlist sheet file path: {}", outputPath.toAbsolutePath());
        Files.writeString(outputPath, renderer.render(gig, pages), StandardCharsets.UTF_8);
    }
}
