package com.pourchoices.chordpro.adapter.in.file;

import com.pourchoices.chordpro.application.port.in.GenerateSongCatalogUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Component
@Command(name = "generate-song-catalog", description = "Generates the song catalog")
public class GenerateSongCatalogCommand implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateSongCatalogCommand.class);

    @Autowired
    private GenerateSongCatalogUseCase generateIndexService;

    @Parameters(index = "0", description = "Path to the songs listing.")
    private String songsListingPathString;

    @Override
    public void run() {

        LOGGER.info("Generating song catalog from: {}", songsListingPathString);

        this.generateIndexService.generateIndex(songsListingPathString);

    }
}