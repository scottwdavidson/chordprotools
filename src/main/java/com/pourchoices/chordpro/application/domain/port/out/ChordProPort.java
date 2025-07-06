package com.pourchoices.chordpro.application.domain.port.out;

import com.pourchoices.chordpro.application.domain.model.ParsedSong;

import java.util.List;

public interface ChordProPort {

    List<String> read(String songFilename);

    void write(String songFilename, ParsedSong parsedSong);


}
