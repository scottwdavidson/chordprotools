package com.pourchoices.chordpro.domain.model.model;

import com.pourchoices.chordpro.application.domain.model.ParsedSong;
import com.pourchoices.chordpro.application.domain.service.SongLineParser;
import com.pourchoices.chordpro.application.domain.service.SongParser;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import static com.pourchoices.chordpro.domain.model.model.SongParserTestData.TEST_CHORDPRO_PARSED_FILE;

@ExtendWith(MockitoExtension.class)
@Slf4j
public class SongParserTest {

    private final static String TEST_CHORDPRO_FILENAME = "/Users/scott/PARA/_resources/cho/T" +
        "/ThomasRhett/HalfOfMe.cho";
    @Spy
    private SongLineParser songLineParser = new SongLineParser();

    @InjectMocks
    private SongParser songParser = new SongParser(songLineParser);

    @Test
    void testParseSong() {

        ParsedSong parsedSong = songParser.parse(TEST_CHORDPRO_FILENAME, TEST_CHORDPRO_PARSED_FILE);

        log.info("Parsed header as string: {}", parsedSong.getParsedHeader());
        log.info("Parsed song: {}", parsedSong);

    }

}
