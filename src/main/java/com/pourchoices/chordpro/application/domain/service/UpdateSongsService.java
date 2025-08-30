package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.*;
import com.pourchoices.chordpro.application.port.in.UpdateSongsUseCase;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor(onConstructor_ = @__(@Autowired))
public class UpdateSongsService implements UpdateSongsUseCase {

    private final ReadSongListService readSongListService;
    private final UpdateSongService updateSongService;

    @Override
    public void updateSongs(String chordproSongPathString) {

        ChordProFileListing chordProFileListing = readSongListService.readSongList(chordproSongPathString);

        for (String chordProSongPathString : chordProFileListing.getChordProFileNames()) {

            this.updateSongService.updateSong(chordProSongPathString);
        }
    }
}
