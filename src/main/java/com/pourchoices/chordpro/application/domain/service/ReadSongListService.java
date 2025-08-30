package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.adapter.out.file.SongListingFileReader;
import com.pourchoices.chordpro.application.domain.model.*;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor(onConstructor_ = @__(@Autowired))
public class ReadSongListService  {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReadSongListService.class);

    private final SongListingFileReader songListingFileReader;

    public ChordProFileListing readSongList(String songsListingPathString){

        // read the song catalog path string file
        return this.songListingFileReader.read(songsListingPathString);

    }
}
