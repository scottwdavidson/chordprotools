package com.pourchoices.chordpro.domain.model.model;

import com.pourchoices.chordpro.application.domain.model.ParsedHeaderLine;
import com.pourchoices.chordpro.application.domain.model.ParsedSongPhrase;
import com.pourchoices.chordpro.application.domain.service.SongLineParser;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

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
                arguments(SongLineParserTestData.TITLE_01, SongLineParserTestData.TITLE_EXPECTED_PARSED_LINE_01),
                arguments(SongLineParserTestData.ARTIST_01, SongLineParserTestData.ARTIST_EXPECTED_PARSED_LINE_01),
                arguments(SongLineParserTestData.KEY_01, SongLineParserTestData.KEY_EXPECTED_PARSED_LINE_01),
                arguments(SongLineParserTestData.DURATION_01, SongLineParserTestData.DURATION_EXPECTED_PARSED_LINE_01),
                arguments(SongLineParserTestData.TEMPO_01, SongLineParserTestData.TEMPO_EXPECTED_PARSED_LINE_01),
                arguments(SongLineParserTestData.NORD_01, SongLineParserTestData.NORD_EXPECTED_PARSED_LINE_01),
                arguments(SongLineParserTestData.ROLAND_01, SongLineParserTestData.ROLAND_EXPECTED_PARSED_LINE_01),
                arguments(SongLineParserTestData.VERSION_01,
                    SongLineParserTestData.VERSION_EXPECTED_PARSED_LINE_01),
            arguments(SongLineParserTestData.COUNTIN_01,
                SongLineParserTestData.COUNTIN_EXPECTED_PARSED_LINE_01),
            arguments(SongLineParserTestData.BACKING_01,
                SongLineParserTestData.BACKING_EXPECTED_PARSED_LINE_01),
            arguments(SongLineParserTestData.VE_01,
                SongLineParserTestData.VE_EXPECTED_PARSED_LINE_01),
            arguments(SongLineParserTestData.PERFORMANCE_KEY_01,
                SongLineParserTestData.PERFORMANCE_KEY_EXPECTED_PARSED_LINE_01)

        );
    }

    // Static method to provide test data
    static Stream<Arguments> songTestData() {
        return Stream.of(
                arguments(SongLineParserTestData.DOCUMENT_COMMENT_01, SongLineParserTestData.DOCUMENT_COMMENT_EXPECTED_PARSED_PHRASE_01),
                arguments(SongLineParserTestData.SONG_COMMENT_01, SongLineParserTestData.SONG_COMMENT_EXPECTED_PARSED_PHRASE_01),
                arguments(SongLineParserTestData.START_OF_VERSE_01, SongLineParserTestData.START_OF_VERSE_EXPECTED_PARSED_PHRASE_01),
                arguments(SongLineParserTestData.START_OF_CHORUS_01, SongLineParserTestData.START_OF_CHORUS_EXPECTED_PARSED_PHRASE_01)
        );
    }

}
