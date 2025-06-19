package com.pourchoices.chordpro.domain.model;

import com.pourchoices.chordpro.adapter.in.file.ChordProFileReader;
import com.pourchoices.chordpro.adapter.out.file.ChordProFileWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HeaderFixer {

    private static final Logger LOGGER = LoggerFactory.getLogger(HeaderFixer.class);
    private final SongParser songParser;
    private final ChordProFileReader chordProFileReader;
    private final ChordProFileWriter chordProFileWriter;

    public HeaderFixer(SongParser songParser, ChordProFileReader chordProFileReader, ChordProFileWriter chordProFileWriter) {
        this.songParser = songParser;
        this.chordProFileReader = chordProFileReader;
        this.chordProFileWriter = chordProFileWriter;
    }

    public void fix(String songFilename) {

        // read the song file and parse it
        List<String> songAsStringLines = this.chordProFileReader.read(songFilename);
        ParsedSong parsedSong = this.songParser.parse(songAsStringLines);

        // add missing mandatory header directives ( w/ default values )

        // write the updated song file
        this.chordProFileWriter.write(songFilename, parsedSong);

    }

//    public static void main(String[] args) {
//
//        String songFilename = args[0];
//        LOGGER.info("Input filename: {}", songFilename);
//        fix(songFilename);
//    }
}
