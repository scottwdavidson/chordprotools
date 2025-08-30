package com.pourchoices.chordpro.adapter.in.file;

import com.pourchoices.chordpro.application.port.in.UpdateSongsUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;


@Component
@Command(name = "update-songs", description = "Updates a list of specific chordpro files based on the catalog")
public class UpdateSongsCommand implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateSongsCommand.class);

    @Parameters(index = "0", description = "Path to the songs listing holding songs to be updated.")
    private String songsListingPathString;

    @Autowired
    private UpdateSongsUseCase updateSongsService;

    @Override
    public void run() {

        LOGGER.info("Updating Songs from : {}", songsListingPathString);

        this.updateSongsService.updateSongs(songsListingPathString);
    }
}