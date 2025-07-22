package com.pourchoices.chordpro.adapter.out.file;

import com.pourchoices.chordpro.application.domain.model.ChordProFileListing;
import com.pourchoices.chordpro.application.domain.service.SongLineParser;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;

/**
 * Reader which reads the songs listing file and returns them in an ordered List for processing.
 */
@Service
public class SongListingFileReader {

    private final static Logger LOGGER = LoggerFactory.getLogger(SongLineParser.class);

    @SneakyThrows
    public ChordProFileListing read(String songsListingPathString) {

        ChordProFileListing.ChordProFileListingBuilder builder = ChordProFileListing.builder();

        try (BufferedReader reader = new BufferedReader(new FileReader(songsListingPathString))) {

            String songFilename;

            // Read lines until the end of the file (readLine() returns null)
            while ((songFilename = reader.readLine()) != null) {
                LOGGER.info("songFilename: {}", songFilename);
                builder.chordProFileName(songFilename.trim());
            }
        }

        return builder.build();

    }
}
