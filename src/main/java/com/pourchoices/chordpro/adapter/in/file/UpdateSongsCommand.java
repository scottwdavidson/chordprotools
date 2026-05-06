package com.pourchoices.chordpro.adapter.in.file;

import com.pourchoices.chordpro.application.port.in.UpdateSongsUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Component
@Command(name = "update-songs", description = "Updates a list of specific chordpro files based on the catalog")
@Slf4j
public class UpdateSongsCommand implements Runnable {

    @Parameters(index = "0", description = "Path to the songs listing holding songs to be updated.")
    private String songsListingPathString;

    private final UpdateSongsUseCase updateSongsUseCase;

    public UpdateSongsCommand(UpdateSongsUseCase updateSongsUseCase) {
        this.updateSongsUseCase = updateSongsUseCase;
    }

    @Override
    public void run() {
        log.info("Updating songs from: {}", songsListingPathString);
        this.updateSongsUseCase.updateSongs(songsListingPathString);
    }
}
