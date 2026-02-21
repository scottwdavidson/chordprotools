package com.pourchoices.chordpro.domain.model.model;

import com.pourchoices.chordpro.application.domain.model.ParsedHeader;
import com.pourchoices.chordpro.application.domain.model.ParsedHeaderLine;

import java.util.List;

public interface SongParserTestData extends SongLineParserTestData {

    // School's Out Song File
    public final static List<String> SONG_FILE_01 = List.of(
            TITLE_01, ARTIST_01, KEY_01, DURATION_01, TEMPO_01, NORD_01, ROLAND_01, VERSION_01, DOCUMENT_COMMENT_01, SONG_COMMENT_01, START_OF_VERSE_01, START_OF_CHORUS_01);

    public final static List<ParsedHeaderLine> EXPECTED_PARSED_SONG_FILE_01 = List.of(
            TITLE_EXPECTED_PARSED_LINE_01,
            ARTIST_EXPECTED_PARSED_LINE_01,
            KEY_EXPECTED_PARSED_LINE_01,
            DURATION_EXPECTED_PARSED_LINE_01,
            TEMPO_EXPECTED_PARSED_LINE_01,
            NORD_EXPECTED_PARSED_LINE_01,
            ROLAND_EXPECTED_PARSED_LINE_01,
            VERSION_EXPECTED_PARSED_LINE_01
    );

    public final static ParsedHeader EXPECTED_PARSED_HEADER_01 =
            ParsedHeader.builder()
                    .chordProFilename("./cho/ABC/A/AliceCooper/SchoolsOutForSummer.cho")
                    .headerLine(TITLE_EXPECTED_PARSED_LINE_01)
                    .headerLine(ARTIST_EXPECTED_PARSED_LINE_01)
                    .headerLine(KEY_EXPECTED_PARSED_LINE_01)
                    .headerLine(DURATION_EXPECTED_PARSED_LINE_01)
                    .headerLine(TEMPO_EXPECTED_PARSED_LINE_01)
                    .headerLine(NORD_EXPECTED_PARSED_LINE_01)
                    .headerLine(VERSION_EXPECTED_PARSED_LINE_01)
                    .build();

    public final static List<String> TEST_CHORDPRO_PARSED_FILE = List.of(
            "{title: Half Of Me}",
            "{artist: Thomas Rhett}",
            "{key: G}",
            "{tempo: 112}",
            "{time: 4/4}",
            "{duration: 2:45}",
            "{meta: nord: P55}",
            "{meta: countin: 8}",
            "{meta: backing: 67}",
            "{meta: performance: Gbm}",
            "  ",
            "{c:***********************************************}",
            "{c:** meta: nord: P55}",
            "{c:** meta: countin: 8}",
            "{c:** meta: backing: 67}",
            "{c:** meta: performance: Gbm}",
            "{c:***********************************************}",
            "  ",
            "{c: Intro}",
            "| Am ... | D ... | G ... | . |",
            "  ",
            "  ",
            "  ",
            "  ",
            "  ",
            "  ",
            "{start_of_verse}",
            "[G] I'm supposed to mow the [Am]grass today",
            "[D] I'm supposed to fix the [G]fence",
            "But with the [G]sun beaten [Am]down on me",
            "[D] It's hard to make it make [G]sense",
            "{end_of_verse}");

}
