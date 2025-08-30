package com.pourchoices.chordpro.application.port.out;

import com.pourchoices.chordpro.application.domain.model.ParsedSong;

import java.nio.file.Path;
import java.util.List;

public interface ChordProPort {

    List<String> read(Path chordproSongPath);

    void write(Path chordproSongPath, ParsedSong parsedSong);


}
