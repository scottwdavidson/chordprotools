package com.pourchoices.chordpro.adapter.out.file;

import com.pourchoices.chordpro.application.domain.model.ParsedSong;
import com.pourchoices.chordpro.application.domain.port.out.ChordProPort;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;

@Service
public class ChordProAdapter implements ChordProPort {

    private ChordProFileReader chordProFileReader;
    private ChordProFileWriter chordProFileWriter;

    @Override
    public List<String> read(Path chordproSongPath) {
        return this.chordProFileReader.read(chordproSongPath);
    }

    @Override
    public void write(Path chordproSongPath, ParsedSong parsedSong) {
        this.chordProFileWriter.write(chordproSongPath, parsedSong);
    }
}
