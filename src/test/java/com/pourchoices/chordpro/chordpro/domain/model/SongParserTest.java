package com.pourchoices.chordpro.chordpro.domain.model;

import com.pourchoices.chordpro.domain.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Stream;

import static com.pourchoices.chordpro.chordpro.domain.model.SongParserTestData.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class SongParserTest {

    private final static Logger LOGGER = LoggerFactory.getLogger(SongParserTest.class);
    private final SongParser songParser = new SongParser(new SongLineParser());

    @Test
    void testParseSong() {

        ParsedSong parsedSong = songParser.parse(HALF_OF_ME);

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
