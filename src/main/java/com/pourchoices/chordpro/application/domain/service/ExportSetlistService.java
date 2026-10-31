package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.Setlist;
import com.pourchoices.chordpro.application.port.in.ExportSetlistUseCase;
import com.pourchoices.chordpro.application.port.out.SetlistPort;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Assembles the gig's {@link Setlist} (see {@link SetlistAssembler}) and writes it
 * to a setlist CSV. The CSV projection is delegated entirely to {@link SetlistPort}.
 */
@Service
@AllArgsConstructor(onConstructor_ = @__(@Autowired))
@Slf4j
public class ExportSetlistService implements ExportSetlistUseCase {

    private final SetlistAssembler assembler;
    private final SetlistPort setlistPort;

    @Override
    public Setlist exportSetlist(String gigParam, String outputPathString, boolean includeBackup) {
        Setlist setlist = assembler.assemble(gigParam, includeBackup);

        Path outputPath = Paths.get(outputPathString);
        setlistPort.writeSetlistToCsv(outputPath, setlist.getEntries());
        log.info("Setlist ({} songs) written to {}", setlist.size(), outputPath.toAbsolutePath());

        return setlist;
    }
}
