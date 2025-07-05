package com.pourchoices.chordpro.adapter.out.file;

import com.pourchoices.chordpro.application.domain.model.ChordProFileListing;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reader which reads the songs listing file and returns them in an ordered List for processing.
 */
@Service
public class SongListingFileReader {

    @SneakyThrows
    public ChordProFileListing read(String songsListingPathString) {

        ChordProFileListing.ChordProFileListingBuilder builder = ChordProFileListing.builder();

        try (BufferedReader reader = new BufferedReader(new FileReader(songsListingPathString))) {

            String songFilename;

            // Read lines until the end of the file (readLine() returns null)
            while ((songFilename = reader.readLine()) != null) {
                builder.chordProFileName(songFilename.trim());
            }
        }

        return ChordProFileListing.builder().build();
    }
}
