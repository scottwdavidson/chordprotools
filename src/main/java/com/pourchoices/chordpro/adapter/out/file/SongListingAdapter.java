package com.pourchoices.chordpro.adapter.out.file;

import com.pourchoices.chordpro.application.domain.model.ChordProFileListing;
import com.pourchoices.chordpro.application.port.out.SongListingPort;
import org.springframework.stereotype.Service;

@Service
public class SongListingAdapter implements SongListingPort {

    private final SongListingFileReader songListingFileReader;

    public SongListingAdapter(SongListingFileReader songListingFileReader) {
        this.songListingFileReader = songListingFileReader;
    }

    @Override
    public ChordProFileListing readSongListing(String songsListingPathString) {
        return this.songListingFileReader.read(songsListingPathString);
    }
}
