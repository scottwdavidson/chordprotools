package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.Setlist;
import com.pourchoices.chordpro.application.domain.model.SetlistEntry;
import com.pourchoices.chordpro.application.port.in.PrintSetlistUseCase;
import com.pourchoices.chordpro.application.port.out.SetlistSheetPort;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Assembles the fan-facing setlist (Z-set backups excluded — they'd never fit on two
 * pages and aren't played in order anyway), splits it into two pages, and hands it
 * to {@link SetlistSheetPort} for rendering.
 */
@Service
@AllArgsConstructor(onConstructor_ = @__(@Autowired))
@Slf4j
public class PrintSetlistService implements PrintSetlistUseCase {

    private final SetlistAssembler assembler;
    private final SetlistPageSplitter pageSplitter;
    private final SetlistSheetPort sheetPort;

    @Override
    public Setlist printSetlist(String gigParam, String outputPathString) {
        Setlist setlist = assembler.assemble(gigParam, false);
        List<List<SetlistEntry>> pages = pageSplitter.splitInTwo(setlist.getEntries());

        Path outputPath = Paths.get(outputPathString);
        sheetPort.writeSetlistSheet(outputPath, setlist.getGig(), pages);
        log.info("Printable setlist ({} songs, {} page(s)) written to {}",
                setlist.size(), pages.size(), outputPath.toAbsolutePath());

        return setlist;
    }
}
