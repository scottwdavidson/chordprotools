package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.ChordProFileListing;
import com.pourchoices.chordpro.application.domain.model.ParsedSong;
import com.pourchoices.chordpro.application.port.out.ChordProPort;
import com.pourchoices.chordpro.application.port.out.SongListingPort;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
@AllArgsConstructor(onConstructor_ = @__(@Autowired))
@Slf4j
public class HeaderFixer {

    private final SongParser songParser;
    private final SongListingPort songListingPort;
    private final ChordProPort chordProPort;

    public void fix(String songsFilename) {

        ChordProFileListing chordProFileListing = this.songListingPort.readSongListing(songsFilename);

        log.info("ChordProFileListing: {}", chordProFileListing);

        for (String songFilename : chordProFileListing.getChordProFileNames()) {
            log.info("Fixing : {}", songFilename);
            fixSong(songFilename);
        }
    }

    public void fixSong(String chordproSongFilename) {

        Path chordproSongPath = Paths.get(chordproSongFilename);
        List<String> songAsStringLines = this.chordProPort.read(chordproSongPath);
        ParsedSong parsedSong = this.songParser.parse(chordproSongFilename, songAsStringLines);

        // add missing mandatory header directives ( w/ default values )

        this.chordProPort.write(chordproSongPath, parsedSong);
    }
}
