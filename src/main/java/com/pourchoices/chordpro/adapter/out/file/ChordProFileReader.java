package com.pourchoices.chordpro.adapter.out.file;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads a ChordPro song file into a list of raw lines for downstream processing.
 */
@Service
@Slf4j
public class ChordProFileReader {

    public List<String> read(Path chordproSongPath) {

        List<String> songFile = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(chordproSongPath.toFile()))) {

            String line;
            while ((line = reader.readLine()) != null) {
                songFile.add(line);
            }

        } catch (IOException e) {
            log.error("Failed to read chordpro file: {}", chordproSongPath, e);
        }

        return songFile;
    }
}
