package com.pourchoices.chordpro.chordpro.domain.model;

import com.pourchoices.chordpro.domain.model.*;
import com.pourchoices.chordpro.domain.model.ParsedHeaderLine;

public interface SongLineParserTestData {

    // Title
    public final static String TITLE_NAME_01 = "School's Out For Summer";
    public final static String TITLE_01 = "{title: " + TITLE_NAME_01 + "}";
    public final static ParsedHeaderLine TITLE_EXPECTED_PARSED_LINE_01 =
            ParsedHeaderLine.builder()
                    .headerDirective(HeaderDirective.TITLE)
                    .value(TITLE_NAME_01)
                    .build();

    // Artist
    public final static String ARTIST_NAME_01 = "Alice Cooper";
    public final static String ARTIST_01 = "{artist: " + ARTIST_NAME_01 + "}";
    public final static ParsedHeaderLine ARTIST_EXPECTED_PARSED_LINE_01 =
            ParsedHeaderLine.builder()
                    .headerDirective(HeaderDirective.ARTIST)
                    .value(ARTIST_NAME_01)
                    .build();

    // Key
    public final static String KEY_VALUE_01 = "Am";
    public final static String KEY_01 = "{key: " + KEY_VALUE_01 + "}";
    public final static ParsedHeaderLine KEY_EXPECTED_PARSED_LINE_01 =
            ParsedHeaderLine.builder()
                    .headerDirective(HeaderDirective.KEY)
                    .value(KEY_VALUE_01)
                    .build();

    // Duration
    public final static String DURATION_VALUE_01 = "3:45";
    public final static String DURATION_01 = "{duration: " + DURATION_VALUE_01 + "}";
    public final static ParsedHeaderLine DURATION_EXPECTED_PARSED_LINE_01 =
            ParsedHeaderLine.builder()
                    .headerDirective(HeaderDirective.DURATION)
                    .value(DURATION_VALUE_01)
                    .build();

    // Tempo
    public final static String TEMPO_VALUE_01 = "101";
    public final static String TEMPO_01 = "{tempo: " + TEMPO_VALUE_01 + "}";
    public final static ParsedHeaderLine TEMPO_EXPECTED_PARSED_LINE_01 =
            ParsedHeaderLine.builder()
                    .headerDirective(HeaderDirective.TEMPO)
                    .value(TEMPO_VALUE_01)
                    .build();

    // Nord
    public final static String NORD_VALUE_01 = "P45";
    public final static String NORD_01 = "{meta: nord: " + NORD_VALUE_01 + "}";
    public final static ParsedHeaderLine NORD_EXPECTED_PARSED_LINE_01 =
            ParsedHeaderLine.builder()
                    .headerDirective(HeaderDirective.NORD)
                    .value(NORD_VALUE_01)
                    .build();

    // Version
    public final static String VERSION_VALUE_01 = "1.0";
    public final static String VERSION_01 = "{meta: version: " + VERSION_VALUE_01 + "}";
    public final static ParsedHeaderLine VERSION_EXPECTED_PARSED_LINE_01 =
            ParsedHeaderLine.builder()
                    .headerDirective(HeaderDirective.VERSION)
                    .value(VERSION_VALUE_01)
                    .build();

    // Document Comment
    public final static String DOCUMENT_COMMENT_COMMENT_01 = "this song is about ... ";
    public final static String DOCUMENT_COMMENT_01 = "{{"+ DOCUMENT_COMMENT_COMMENT_01 + "}}";
    public final static ParsedSongPhrase DOCUMENT_COMMENT_EXPECTED_PARSED_PHRASE_01 =
            ParsedSongPhrase.builder()
                    .songDirective(SongDirective.DOCUMENT_COMMENT)
                    .line(DOCUMENT_COMMENT_COMMENT_01)
                    .build();

    // Song Comment
    public final static String SONG_COMMENT_COMMENT_01 = "play 3 times";
    public final static String SONG_COMMENT_01 = "{comment:"+ SONG_COMMENT_COMMENT_01 + "}";
    public final static ParsedSongPhrase SONG_COMMENT_EXPECTED_PARSED_PHRASE_01 =
            ParsedSongPhrase.builder()
                    .songDirective(SongDirective.SONG_COMMENT)
                    .line(SONG_COMMENT_COMMENT_01)
                    .build();

    // Start of Verse
    public final static String START_OF_VERSE_01 = "{start_of_verse}";
    public final static ParsedSongPhrase START_OF_VERSE_EXPECTED_PARSED_PHRASE_01 =
            ParsedSongPhrase.builder()
                    .songDirective(SongDirective.VERSE)
                    .build();

    // Start of Chorus
    public final static String START_OF_CHORUS_01 = "{soc}";
    public final static ParsedSongPhrase START_OF_CHORUS_EXPECTED_PARSED_PHRASE_01 =
            ParsedSongPhrase.builder()
                    .songDirective(SongDirective.CHORUS)
                    .build();

}
