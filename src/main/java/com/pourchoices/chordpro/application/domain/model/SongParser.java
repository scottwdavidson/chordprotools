package com.pourchoices.chordpro.application.domain.model;

import org.springframework.stereotype.Service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parsing a chordpro song ( file ) in the form of a java.util.List of java.lang.String objects
 * into a ParsedSong object.
 *
 */
@Service
public class SongParser {

    private final static Logger LOGGER = LoggerFactory.getLogger(SongParser.class);
    private final SongLineParser songLineParser;

    public SongParser(SongLineParser songLineParser) {
        this.songLineParser = songLineParser;
    }

    public ParsedSong parse(List<String> songFile) {

        ParsedSong.ParsedSongBuilder parsedSongBuilder = ParsedSong.builder();

        // header first
        ParsedHeader.ParsedHeaderBuilder headerBuilder = ParsedHeader.builder();

        // iterate until you hit a non HEADER line
        int lineIndex = 0;
        while (lineIndex < songFile.size()) {

            String line = songFile.get(lineIndex);

            if(line.isBlank()) {
                lineIndex++;
                continue;
            }

            ParsedHeaderLine parsedHeaderLine =
                    this.songLineParser.parseHeaderLine(line);
            if (parsedHeaderLine != null) {
                headerBuilder.headerLine(parsedHeaderLine);
                lineIndex++;
            }
            else {
                break;
            }
        }

        ParsedHeader parsedHeader = headerBuilder.build();
        parsedSongBuilder.parsedHeader(parsedHeader);

        // copy the rest of the lines verbatim
        while (lineIndex < songFile.size()) {

            String line = songFile.get(lineIndex);
            parsedSongBuilder.line(line);
            lineIndex++;
        }

        return parsedSongBuilder.build();
    }

}
