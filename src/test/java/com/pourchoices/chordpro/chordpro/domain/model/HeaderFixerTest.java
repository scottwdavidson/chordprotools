package com.pourchoices.chordpro.chordpro.domain.model;

import com.pourchoices.chordpro.domain.model.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static com.pourchoices.chordpro.chordpro.domain.model.SongParserTestData.EXPECTED_PARSED_SONG_FILE_01;
import static com.pourchoices.chordpro.chordpro.domain.model.SongParserTestData.SONG_FILE_01;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class HeaderFixerTest {

//    private final HeaderFixer headerFixer = new HeaderFixer(
//            new HeaderFixer());

//    @ParameterizedTest
//    @MethodSource("songTestData") // Reference the method that provides the data
//    void testParse(List<String> song, List<ParsedSongPhrase> expectedParsedSong) {
//
//        headerFixer.fix(expectedParsedSong);
//    }
//
//    // Static method to provide test data
//    static Stream<Arguments> songTestData() {
//        return Stream.of(
//                arguments(SONG_FILE_01, EXPECTED_PARSED_SONG_FILE_01)
//        );
//    }

}
