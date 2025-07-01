package com.pourchoices.chordpro.adapter.out.file;

import com.pourchoices.chordpro.application.domain.model.ParsedSong;
import org.springframework.stereotype.Service;

import java.io.*;

/**
 * Reader which reads the entire ChordPro file into a list of "lines" to then be processed
 */
@Service
public class ChordProFileWriter {

    public void write(String songFilename, ParsedSong parsedSong) {

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(songFilename, false))) { // Using false for overwrite

            writer.write(parsedSong.toString());

        } catch (IOException ioException) {
            // Handle the exception, e.g., print stack trace or rethrow.
            ioException.printStackTrace();
        }
    }
}
