package com.pourchoices.chordpro.adapter.in.file;

import com.pourchoices.chordpro.application.port.in.UpdateSongUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;


@Component
@Command(name = "update-song", description = "Updates a specific chord sheet based on the catalog")
public class UpdateSongCommand implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateSongCommand.class);

    @Parameters(index = "0", description = "Path to the song to be updated.")
    private String chordproSongPathString;

    @Autowired
    private UpdateSongUseCase updateSongService;

    @Override
    public void run() {

        // Your index generation logic here, using this.inputFile
        LOGGER.info("Updating Song: {}", chordproSongPathString);

        this.updateSongService.updateSong(chordproSongPathString);
    }
}