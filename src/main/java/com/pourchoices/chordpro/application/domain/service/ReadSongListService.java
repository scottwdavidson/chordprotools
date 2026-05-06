package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.ChordProFileListing;
import com.pourchoices.chordpro.application.port.out.SongListingPort;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor(onConstructor_ = @__(@Autowired))
public class ReadSongListService {

    private final SongListingPort songListingPort;

    public ChordProFileListing readSongList(String songsListingPathString) {
        return this.songListingPort.readSongListing(songsListingPathString);
    }
}
