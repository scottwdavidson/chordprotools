package com.pourchoices.chordpro.adapter.out.file;

import com.pourchoices.chordpro.application.domain.model.ParsedSong;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Path;

/**
 * Reader which writes the ParsedSong to the specified chordpro filename
 */
@Service
public class ChordProFileWriter {

    public void write(Path chordproSongPath, ParsedSong parsedSong) {

        // TODO shouldn't I be passing a Path ?
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(chordproSongPath.toFile(), false))) { // Using false for overwrite

            writer.write(parsedSong.toString());

        } catch (IOException ioException) {
            // Handle the exception, e.g., print stack trace or rethrow.
            ioException.printStackTrace();
        }
    }
}
