package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.adapter.out.file.CatalogEntryDto;
import com.pourchoices.chordpro.adapter.out.file.CatalogFileWriter;
import com.pourchoices.chordpro.adapter.out.file.ChordProFileReader;
import com.pourchoices.chordpro.adapter.out.file.SongListingFileReader;
import com.pourchoices.chordpro.application.domain.model.*;
import com.pourchoices.chordpro.application.domain.port.in.GenerateIndexUseCase;
import com.pourchoices.chordpro.config.ChordproCatalogIndexPathConfig;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

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
