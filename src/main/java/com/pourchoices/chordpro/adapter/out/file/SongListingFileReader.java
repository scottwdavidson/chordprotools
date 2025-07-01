package com.pourchoices.chordpro.adapter.out.file;

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
    public List<String> read(String songsListingPathString) {

        List<String> songsListing = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(songsListingPathString))) {

            String songFilename;

            // Read lines until the end of the file (readLine() returns null)
            while ((songFilename = reader.readLine()) != null) {
                songsListing.add(songFilename.trim());
            }
        }

        return songsListing;
    }
}
