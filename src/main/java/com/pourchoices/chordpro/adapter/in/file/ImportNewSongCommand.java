package com.pourchoices.chordpro.adapter.in.file;

import com.pourchoices.chordpro.application.port.in.ImportNewSongUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;


@Component
@Command(name = "import-new-song", description = "Imports new song into the song catalog")
public class ImportNewSongCommand implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImportNewSongCommand.class);

    @Parameters(index = "0", description = "Path to the song to be imported.")
    private String chordproSongPathString;

    @Autowired
    private ImportNewSongUseCase importNewSongService;

    @Override
    public void run() {

        LOGGER.info("Updating Catalog from: {}", chordproSongPathString);

        this.importNewSongService.importNewSong(chordproSongPathString);
    }
}
