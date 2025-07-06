package com.pourchoices.chordpro.adapter.out.file;

import com.pourchoices.chordpro.application.domain.model.ParsedSong;
import com.pourchoices.chordpro.application.domain.port.out.ChordProPort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChordProAdapter implements ChordProPort {

    private ChordProFileReader chordProFileReader;
    private ChordProFileWriter chordProFileWriter;

    @Override
    public List<String> read(String songFilename) {
        return this.chordProFileReader.read(songFilename);
    }

    @Override
    public void write(String songFilename, ParsedSong parsedSong) {
        this.chordProFileWriter.write(songFilename, parsedSong);
    }
}
