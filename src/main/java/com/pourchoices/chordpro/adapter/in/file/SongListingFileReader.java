package com.pourchoices.chordpro.adapter.in.file;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reader which reads the songs listing file and returns them in an ordered List
 * for processing.
 */
@Service
public class SongListingFileReader {

    public List<String> read(String songsFilename) {

        List<String> songsListing = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(songsFilename))) {

            String songFilename;

            // Read lines until the end of the file (readLine() returns null)
            while ((songFilename = reader.readLine()) != null) {
                songsListing.add(songFilename.trim());
            }
        } catch (IOException e) {
            // Handle any potential I/O errors (e.g., file not found, permissions issues)
            e.printStackTrace();
        }

        return songsListing;
    }
}
