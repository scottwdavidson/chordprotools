package com.pourchoices.chordpro.adapter.in.file;

import com.pourchoices.chordpro.application.port.in.ImportNewSongUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Component
@Command(name = "import-new-song", description = "Imports new song into the song catalog")
@Slf4j
public class ImportNewSongCommand implements Runnable {

    @Parameters(index = "0", description = "Path to the song to be imported.")
    private String chordproSongPathString;

    private final ImportNewSongUseCase importNewSongUseCase;

    public ImportNewSongCommand(ImportNewSongUseCase importNewSongUseCase) {
        this.importNewSongUseCase = importNewSongUseCase;
    }

    @Override
    public void run() {
        log.info("Importing song: {}", chordproSongPathString);
        this.importNewSongUseCase.importNewSong(chordproSongPathString);
    }
}
