package com.pourchoices.chordpro.adapter.out.file;

import com.pourchoices.chordpro.application.domain.model.ParsedSong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Writes a {@link ParsedSong} back to its chordpro file, overwriting the existing content.
 */
@Service
@Slf4j
public class ChordProFileWriter {

    public void write(Path chordproSongPath, ParsedSong parsedSong) {

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(chordproSongPath.toFile(), false))) {
            writer.write(parsedSong.toString());
        } catch (IOException e) {
            log.error("Failed to write chordpro file: {}", chordproSongPath, e);
        }
    }
}
