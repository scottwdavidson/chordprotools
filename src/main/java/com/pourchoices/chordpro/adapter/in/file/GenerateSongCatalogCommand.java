package com.pourchoices.chordpro.adapter.in.file;

import com.pourchoices.chordpro.application.port.in.GenerateSongCatalogUseCase;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Component
@Command(name = "generate-song-catalog", description = "Generates the song catalog")
@Slf4j
public class GenerateSongCatalogCommand implements Runnable {

    @Autowired
    private GenerateSongCatalogUseCase generateIndexService;

    @Parameters(index = "0", description = "Path to the songs listing.")
    private String songsListingPathString;

    @Override
    public void run() {

        log.info("Generating song catalog from: {}", songsListingPathString);

        this.generateIndexService.generateSongCatalog(songsListingPathString);

    }
}