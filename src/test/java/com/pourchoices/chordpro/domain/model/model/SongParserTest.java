package com.pourchoices.chordpro.domain.model.model;

import com.pourchoices.chordpro.application.domain.model.ParsedSong;
import com.pourchoices.chordpro.application.domain.service.SongLineParser;
import com.pourchoices.chordpro.application.domain.service.SongParser;
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
public class SongParserTest {

    private final static Logger LOGGER = LoggerFactory.getLogger(SongParserTest.class);
//    private final SongParser songParser = new SongParser(new SongLineParser());

    private final static String TEST_CHORDPRO_FILENAME = "/Users/scott/PARA/_resources/cho/T" +
        "/ThomasRhett/HalfOfMe.cho";
    @Spy
    private SongLineParser songLineParser = new SongLineParser();

    @InjectMocks
    private SongParser songParser = new SongParser(songLineParser);

    @Test
    void testParseSong() {

        ParsedSong parsedSong = songParser.parse(TEST_CHORDPRO_FILENAME, TEST_CHORDPRO_PARSED_FILE);

        LOGGER.info("Parsed header as string: {}", parsedSong.getParsedHeader());
        LOGGER.info("Parsed song: {}", parsedSong);

    }
//    @ParameterizedTest
//    @MethodSource("songTestData") // Reference the method that provides the data
//    void testParseHeader(List<String> song, ParsedHeader expectedParsedHeader) {
//
//        ParsedSong parsedSong = songParser.parse(song);
//
//        LOGGER.info("Parsed header as string: {}", parsedSong.getParsedHeader());
//
//    }
//
//    // Static method to provide test data
//    static Stream<Arguments> songTestData() {
//        return Stream.of(
//                arguments(SONG_FILE_01, EXPECTED_PARSED_SONG_FILE_01)
//        );
//    }

}
