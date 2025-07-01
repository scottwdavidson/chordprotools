package com.pourchoices.chordpro.chordpro.domain.model;

import com.pourchoices.chordpro.application.domain.model.ParsedHeaderLine;
import com.pourchoices.chordpro.application.domain.model.ParsedSongPhrase;
import com.pourchoices.chordpro.application.domain.service.SongLineParser;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.pourchoices.chordpro.chordpro.domain.model.SongLineParserTestData.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class SongLineParserTest {

    private final SongLineParser songLineParser = new SongLineParser();

    @ParameterizedTest
    @MethodSource("headerTestData")
        // Reference the method that provides the data
    void testHeaderParsing(String lineContent, ParsedHeaderLine expectedParsedHeaderLine) {

        ParsedHeaderLine parsedHeaderLine = songLineParser.parseHeaderLine(lineContent);

        // Assert that the actual value matches the expected value
        assertEquals(expectedParsedHeaderLine, parsedHeaderLine);
    }

    @ParameterizedTest
    @MethodSource("songTestData")
        // Reference the method that provides the data
    void testSongParsing(String lineContent, ParsedSongPhrase expectedParsedSongPhrase) {

        ParsedSongPhrase parsedSongPhrase = songLineParser.parseSongPhrase(lineContent);

        // Assert that the actual value matches the expected value
        assertEquals(expectedParsedSongPhrase, parsedSongPhrase);
    }


    // Static method to provide test data
    static Stream<Arguments> headerTestData() {
        return Stream.of(
                arguments(TITLE_01, TITLE_EXPECTED_PARSED_LINE_01),
                arguments(ARTIST_01, ARTIST_EXPECTED_PARSED_LINE_01),
                arguments(KEY_01, KEY_EXPECTED_PARSED_LINE_01),
                arguments(DURATION_01, DURATION_EXPECTED_PARSED_LINE_01),
                arguments(TEMPO_01, TEMPO_EXPECTED_PARSED_LINE_01),
                arguments(NORD_01, NORD_EXPECTED_PARSED_LINE_01),
                arguments(VERSION_01, VERSION_EXPECTED_PARSED_LINE_01) );
    }

    // Static method to provide test data
    static Stream<Arguments> songTestData() {
        return Stream.of(
                arguments(DOCUMENT_COMMENT_01, DOCUMENT_COMMENT_EXPECTED_PARSED_PHRASE_01),
                arguments(SONG_COMMENT_01, SONG_COMMENT_EXPECTED_PARSED_PHRASE_01),
                arguments(START_OF_VERSE_01, START_OF_VERSE_EXPECTED_PARSED_PHRASE_01),
                arguments(START_OF_CHORUS_01, START_OF_CHORUS_EXPECTED_PARSED_PHRASE_01)
        );
    }

}
