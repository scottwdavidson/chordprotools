package com.pourchoices.chordpro.adapter.in.file;

import com.pourchoices.chordpro.application.port.in.UpdateSongsUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Component
@Command(name = "update-songs", description = "Updates a list of songs (by song ID) based on the catalog")
@Slf4j
public class UpdateSongsCommand implements Runnable {

    @Parameters(index = "0", description = "Path to the listing file of song IDs to be updated.")
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
