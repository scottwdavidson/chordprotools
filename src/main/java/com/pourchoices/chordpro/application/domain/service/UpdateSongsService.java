package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.ChordProFileListing;
import com.pourchoices.chordpro.application.domain.model.SongId;
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
    public void updateSongs(String songIdsListingPathString) {

        ChordProFileListing chordProFileListing =
                readSongListService.readSongList(songIdsListingPathString);

        for (String songIdString : chordProFileListing.getChordProFileNames()) {

            this.updateSongService.updateSong(SongId.parse(songIdString));
        }
    }
}
