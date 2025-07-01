package com.pourchoices.chordpro.chordpro.domain.model;

import com.pourchoices.chordpro.application.domain.model.ParsedHeader;
import com.pourchoices.chordpro.application.domain.model.ParsedHeaderLine;

import java.util.List;

public interface SongParserTestData extends SongLineParserTestData {

    // School's Out Song File
    public final static List<String> SONG_FILE_01 = List.of(
            TITLE_01, ARTIST_01, KEY_01, DURATION_01, TEMPO_01, NORD_01, VERSION_01, DOCUMENT_COMMENT_01, SONG_COMMENT_01, START_OF_VERSE_01, START_OF_CHORUS_01);

    public final static List<ParsedHeaderLine> EXPECTED_PARSED_SONG_FILE_01 = List.of(
            TITLE_EXPECTED_PARSED_LINE_01,
            ARTIST_EXPECTED_PARSED_LINE_01,
            KEY_EXPECTED_PARSED_LINE_01,
            DURATION_EXPECTED_PARSED_LINE_01,
            TEMPO_EXPECTED_PARSED_LINE_01,
            NORD_EXPECTED_PARSED_LINE_01,
            VERSION_EXPECTED_PARSED_LINE_01
    );

    public final static ParsedHeader EXPECTED_PARSED_HEADER_01 =
            ParsedHeader.builder()
                    .headerLine(TITLE_EXPECTED_PARSED_LINE_01)
                    .headerLine(ARTIST_EXPECTED_PARSED_LINE_01)
                    .headerLine(KEY_EXPECTED_PARSED_LINE_01)
                    .headerLine(DURATION_EXPECTED_PARSED_LINE_01)
                    .headerLine(TEMPO_EXPECTED_PARSED_LINE_01)
                    .headerLine(NORD_EXPECTED_PARSED_LINE_01)
                    .headerLine(VERSION_EXPECTED_PARSED_LINE_01)
                    .build();

    public final static List<String> HALF_OF_ME = List.of(
            "{title: Half Of Me}",
            "{artist: Thomas Rhett}",
            "{key: G}",
            "{tempo: 112}",
            "{time: 4/4}",
            "{duration: 2:45}",
            "{meta: version : 1.0}",
            "{c: nord=N35}",
            "{comment: backing: 11}",
            "{c: countin: 4}",
            "{c: ev = P06}",
            "  ",
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
            "{end_of_verse}",


            "{start_of_chorus}",
            "Half of me wants a [Am]cold beer",
            "[D] Yeah, that's the cold hard [G]truth",
            "[G] And with the frigerator [Am]stocked full of em'",
            "[D]Tell me what's a boy to do [G]",
            "I ain't [Am]even tryna fight it, it's al-[D]ready been decided",
            "The [G]sky and the mountains [D/F#]are blue [Em]",
            "Half of me wants a [Am]cold beer",
            "[D] And the other half does [G]too",
            "{end_of_chorus}",
            "   ",
            "   ",
            "   ",
            "{start_of_verse}",
            "[G] Yeah, I kinda need to [Am]wash my truck",
            "[D] But hell I kinda don’t [G]care",
            "[G#dim] I think ole Alan Jackson [Am]said it best",
            "[D] It's is 5 o clock some-[G]where",
            "{end_of_verse}",
            "   ",
            "   ",
            "   ",
            "{start_of_chorus}",
            "Half of me wants a [Am]cold beer",
            "[D] Yeah, that's the cold hard [G]truth",
            "[G] And with the frigerator [Am]stocked full of em'",
            "[D]Tell me what's a boy to do [G]",
            "I ain't [Am]even tryna fight it, it's al-[D]ready been decided",
            "The [G]sky and the mountains [D/F#]are blue [Em]",
            "Half of me wants a [Am]cold beer",
            "[D]And the other half does [G] too",
            "{end_of_chorus}",
            "   ",
            "   ",
            "   ",
            "{start_of_bridge}",
            "Yeah, yeah[Am]",
            "[D] Half of me wants a [G]cold beer",
            "If I did [Am]what I should be doin' but then [D/F#]that would really ruin all the [Em]fun, yeah",
            "There's a [Am]world of POUR CHOICES out [C]there, but this ain't [D]one",
            "{end_of_bridge}",
            "   ",
            "   ",
            "   ",
            "{start_of_chorus}",
            "Cause half of me wants a [Am]cold, cold beer",
            "[D] Yeah, that's the cold hard [G] truth [Yeah, it is]",
            "[G] And with the 'frigerator [Am]stocked full of 'em",
            "[D]Tell me, what's a boy to do? [G]",
            "I ain't [Am]even tryna fight it, it's al-[D]ready been decided",
            "The [G]sky and the mountains [D/F#]are blue [Em]",
            "Half of me wants a [Am]cold beer",
            "[D] And the other half does [G] too [D/F#]",
            "{end_of_chorus}",
            " ",
            "   ",
            "   ",
            "   ",
            "{c:Outro}",
            "[Em] Yeah, half of me wants a [Am]cold, cold beer",
            "[D] The other half wants two",
            "[G]Woo-hoo!",
            "          ",
            "        ",
            "}");

}
