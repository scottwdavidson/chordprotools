package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.HeaderDirective;
import com.pourchoices.chordpro.application.domain.model.ParsedHeader;
import com.pourchoices.chordpro.application.domain.model.ParsedHeaderLine;
import com.pourchoices.chordpro.application.domain.model.ParsedSong;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
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
@AllArgsConstructor(onConstructor_ = @__(@Autowired))
public class SongParser {

    private final static Logger LOGGER = LoggerFactory.getLogger(SongParser.class);
    private final SongLineParser songLineParser;

    public ParsedSong parse(String chordproFilename, List<String> songFile) {

        ParsedSong.ParsedSongBuilder parsedSongBuilder = ParsedSong.builder();

        // header first
        LOGGER.info("chordproFilename: {}", chordproFilename);
        ParsedHeader.ParsedHeaderBuilder headerBuilder = ParsedHeader.builder();
        headerBuilder.chordProFilename(chordproFilename);

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

                // ignore EPHEMERAL COMMENT lines - they'll be regenerated as needed
                if (!parsedHeaderLine.getHeaderDirective().equals(HeaderDirective.EPHEMERAL_COMMENT)) {
                    headerBuilder.headerLine(parsedHeaderLine);
                }
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
