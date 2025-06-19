package com.pourchoices.chordpro.adapter.in.file;

import lombok.Builder;
import lombok.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reader which reads the entire ChordPro file into a list of "lines" to then be processed
 */
@Service
public class ChordProFileReader {

    public List<String> read(String songFilename) {

        List<String> songFile = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(songFilename))) {

            String line;

            // Read lines until the end of the file (readLine() returns null)
            while ((line = reader.readLine()) != null) {
                songFile.add(line);
            }
        } catch (IOException e) {
            // Handle any potential I/O errors (e.g., file not found, permissions issues)
            e.printStackTrace();
        }

        return songFile;
    }
}
