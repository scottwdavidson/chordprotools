package com.pourchoices.chordpro.application.port.out;

import com.pourchoices.chordpro.application.domain.model.ChordProFileListing;

public interface SongListingPort {

    ChordProFileListing readSongListing(String songsListingPathString);

}
