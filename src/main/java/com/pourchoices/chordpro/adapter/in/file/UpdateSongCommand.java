package com.pourchoices.chordpro.adapter.in.file;

import com.pourchoices.chordpro.application.port.in.UpdateSongUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Component
@Command(name = "update-song", description = "Updates a specific chord sheet based on the catalog")
@Slf4j
public class UpdateSongCommand implements Runnable {

    @Parameters(index = "0", description = "Path to the song to be updated.")
    private String chordproSongPathString;

    private final UpdateSongUseCase updateSongUseCase;

    public UpdateSongCommand(UpdateSongUseCase updateSongUseCase) {
        this.updateSongUseCase = updateSongUseCase;
    }

    @Override
    public void run() {
        log.info("Updating song: {}", chordproSongPathString);
        this.updateSongUseCase.updateSong(chordproSongPathString);
    }
}
