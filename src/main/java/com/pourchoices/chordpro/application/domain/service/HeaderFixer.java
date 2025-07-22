package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.adapter.out.file.ChordProFileReader;
import com.pourchoices.chordpro.adapter.out.file.SongListingFileReader;
import com.pourchoices.chordpro.adapter.out.file.ChordProFileWriter;
import com.pourchoices.chordpro.application.domain.model.ChordProFileListing;
import com.pourchoices.chordpro.application.domain.model.ParsedSong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
public class HeaderFixer {

    private static final Logger LOGGER = LoggerFactory.getLogger(HeaderFixer.class);
    private final SongParser songParser;
    private final SongListingFileReader songListingFileReader;
    private final ChordProFileReader chordProFileReader;
    private final ChordProFileWriter chordProFileWriter;

    public HeaderFixer(SongParser songParser,
                       SongListingFileReader songListingFileReader,
                       ChordProFileReader chordProFileReader,
                       ChordProFileWriter chordProFileWriter) {
        this.songParser = songParser;
        this.songListingFileReader = songListingFileReader;
        this.chordProFileReader = chordProFileReader;
        this.chordProFileWriter = chordProFileWriter;
    }

    public void fix(String songsFilename){

        // read the song listing file
        ChordProFileListing chordProFileListing = this.songListingFileReader.read(songsFilename);

        LOGGER.info("ChordProFileListing: {}", chordProFileListing);

        // iterate through the song listing and fix each song
        for(String songFilename : chordProFileListing.getChordProFileNames()){
            LOGGER.info("Fixing : {}", songFilename);
            fixSong(songFilename);
        }
    }
    public void fixSong(String chordproSongFilename) {

        // read the song file and parse it
        Path chordproSongPath = Paths.get(chordproSongFilename);
        List<String> songAsStringLines = this.chordProFileReader.read(chordproSongPath);
        ParsedSong parsedSong = this.songParser.parse(chordproSongFilename, songAsStringLines);

        // add missing mandatory header directives ( w/ default values )

        // write the updated song file
        this.chordProFileWriter.write(chordproSongPath, parsedSong);

    }

//    public static void main(String[] args) {
//
//        String songFilename = args[0];
//        LOGGER.info("Input filename: {}", songFilename);
//        fix(songFilename);
//    }
}
