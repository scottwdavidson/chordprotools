package com.pourchoices.chordpro.adapter.in.file;

import com.pourchoices.chordpro.application.port.in.GenerateSongCatalogUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Component
@Command(name = "generate-song-catalog", description = "Generates the song catalog")
@Slf4j
public class GenerateSongCatalogCommand implements Runnable {

    @Parameters(index = "0", description = "Path to the songs listing.")
    private String songsListingPathString;

    private final GenerateSongCatalogUseCase generateSongCatalogUseCase;

    public GenerateSongCatalogCommand(GenerateSongCatalogUseCase generateSongCatalogUseCase) {
        this.generateSongCatalogUseCase = generateSongCatalogUseCase;
    }

    @Override
    public void run() {
        log.info("Generating song catalog from: {}", songsListingPathString);
        this.generateSongCatalogUseCase.generateSongCatalog(songsListingPathString);
    }
}
