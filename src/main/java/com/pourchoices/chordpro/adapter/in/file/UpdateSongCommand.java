package com.pourchoices.chordpro.adapter.in.file;

import com.pourchoices.chordpro.application.domain.model.SongId;
import com.pourchoices.chordpro.application.port.in.UpdateSongUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Component
@Command(name = "update-song",
        description = "Updates a song (and all its key-variants) from the catalog, identified by song ID")
@Slf4j
public class UpdateSongCommand implements Runnable {

    @Parameters(index = "0",
            description = "Song ID of the song to update, e.g. ABC:B:BillyJoel:MyLife")
    private String songIdString;

    private final UpdateSongUseCase updateSongUseCase;

    public UpdateSongCommand(UpdateSongUseCase updateSongUseCase) {
        this.updateSongUseCase = updateSongUseCase;
    }

    @Override
    public void run() {
        log.info("Updating song: {}", songIdString);
        this.updateSongUseCase.updateSong(SongId.parse(songIdString));
    }
}
