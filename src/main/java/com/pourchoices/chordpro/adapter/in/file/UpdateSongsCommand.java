package com.pourchoices.chordpro.adapter.in.file;

import com.pourchoices.chordpro.application.port.in.UpdateSongsUseCase;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;


@Component
@Command(name = "update-songs", description = "Updates a list of specific chordpro files based on the catalog")
@Slf4j
public class UpdateSongsCommand implements Runnable {

    @Parameters(index = "0", description = "Path to the songs listing holding songs to be updated.")
    private String songsListingPathString;

    @Autowired
    private UpdateSongsUseCase updateSongsService;

    @Override
    public void run() {

        log.info("Updating Songs from : {}", songsListingPathString);

        this.updateSongsService.updateSongs(songsListingPathString);
    }
}