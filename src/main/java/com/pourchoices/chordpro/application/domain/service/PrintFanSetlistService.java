package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.Setlist;
import com.pourchoices.chordpro.application.domain.model.SetlistEntry;
import com.pourchoices.chordpro.application.port.in.PrintFanSetlistUseCase;
import com.pourchoices.chordpro.application.port.out.FanSetlistSheetPort;
import com.pourchoices.chordpro.application.port.out.GigVenuePort;
import com.pourchoices.chordpro.config.ChordproGigVenuesPathConfig;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Assembles the played setlist (Z-set backups excluded), groups it by set, looks up the
 * gig's venue, and hands it to {@link FanSetlistSheetPort} to become the fans' handout.
 * A gig with no venue linked in {@code gig-venues.csv} simply prints without one.
 */
@Service
@AllArgsConstructor(onConstructor_ = @__(@Autowired))
@Slf4j
public class PrintFanSetlistService implements PrintFanSetlistUseCase {

    private final SetlistAssembler assembler;
    private final SetlistSetSplitter setSplitter;
    private final GigVenuePort gigVenuePort;
    private final ChordproGigVenuesPathConfig gigVenuesConfig;
    private final FanSetlistSheetPort sheetPort;

    @Override
    public Setlist printFanSetlist(String gigParam, String outputPathString) {
        Setlist setlist = assembler.assemble(gigParam, false);
        List<List<SetlistEntry>> sets = setSplitter.splitBySet(setlist.getEntries());
        String venue = gigVenuePort.readGigVenues(Paths.get(gigVenuesConfig.getGigVenuesPath()))
                .get(setlist.getGig());

        Path outputPath = Paths.get(outputPathString);
        sheetPort.writeFanSetlistSheet(outputPath, setlist.getGig(), venue, sets);
        log.info("Fan setlist ({} songs, {} set(s), venue={}) written to {}",
                setlist.size(), sets.size(), venue, outputPath.toAbsolutePath());

        return setlist;
    }
}
