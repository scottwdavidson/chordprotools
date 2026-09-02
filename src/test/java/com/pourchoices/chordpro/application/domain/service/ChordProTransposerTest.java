package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.BracketedLine;
import com.pourchoices.chordpro.application.domain.model.MusicalKey;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ChordProTransposer} — root transposition, slash-chord
 * bass notes, the non-chord-bracket guard, and key-driven accidental spelling.
 */
class ChordProTransposerTest {

    private final ChordProTransposer transposer = new ChordProTransposer();

    @Test
    void transposesSimpleRoot() {
        assertThat(transposer.transpose("[C]Hello", 2, false)).isEqualTo("[D]Hello");
    }

    @Test
    void preservesQualityAndExtensions() {
        assertThat(transposer.transpose("[Fmaj7]word[Gm]", 2, false))
                .isEqualTo("[Gmaj7]word[Am]");
    }

    @Test
    void transposesSlashChordBassNoteToo() {
        // This is the documented gap from consistent-song-data.md §9.1 -
        // previously only the root ([A/E] -> [C/E]) would move.
        assertThat(transposer.transpose("[A/E]shine", 2, false)).isEqualTo("[B/F#]shine");
    }

    @Test
    void slashChordUsesFlatsWhenTargetPrefersFlats() {
        assertThat(transposer.transpose("[A/E]shine", 1, true)).isEqualTo("[Bb/F]shine");
    }

    @Test
    void nonChordBracket_sectionLabels_leftUntouched() {
        assertThat(transposer.transpose("[Bridge]", 2, false)).isEqualTo("[Bridge]");
        assertThat(transposer.transpose("[Chorus]", 2, false)).isEqualTo("[Chorus]");
        assertThat(transposer.transpose("[Bass]", 2, false)).isEqualTo("[Bass]");
        assertThat(transposer.transpose("[Ending]", 2, false)).isEqualTo("[Ending]");
    }

    @Test
    void nonChordBracket_riffNotation_leftUntouched() {
        // Multi-note riff annotations like "[E F# G A]" are not a single chord.
        assertThat(transposer.transpose("[E F# G A]", 2, false)).isEqualTo("[E F# G A]");
    }

    @Test
    void nonChordBracket_guitarTabFragment_leftUntouched() {
        assertThat(transposer.transpose("[E|---3-----5---|]", 2, false))
                .isEqualTo("[E|---3-----5---|]");
    }

    @Test
    void nonChordBracket_countMarker_leftUntouched() {
        assertThat(transposer.transpose("[2x]", 2, false)).isEqualTo("[2x]");
    }

    @Test
    void knownGap_slashExtensionChordIsLeftUntouched() {
        // "C6/9" is a real (if rare) chord notation, but the chosen regex
        // deliberately can't distinguish it from a slash-bass chord without
        // risking false positives elsewhere. Documented limitation - it's
        // left alone rather than mangled.
        assertThat(transposer.transpose("[C6/9]groove", 2, false)).isEqualTo("[C6/9]groove");
    }

    @Test
    void validExtendedQualities_areRecognizedAndTransposed() {
        assertThat(transposer.transpose("[Csus4]", 2, false)).isEqualTo("[Dsus4]");
        assertThat(transposer.transpose("[Cadd9]", 2, false)).isEqualTo("[Dadd9]");
        assertThat(transposer.transpose("[Cdim7]", 2, false)).isEqualTo("[Ddim7]");
        assertThat(transposer.transpose("[Caug]", 2, false)).isEqualTo("[Daug]");
        assertThat(transposer.transpose("[Cm7b5]", 2, false)).isEqualTo("[Dm7b5]");
        assertThat(transposer.transpose("[Cmaj7#9]", 2, false)).isEqualTo("[Dmaj7#9]");
        assertThat(transposer.transpose("[C9no5]", 2, false)).isEqualTo("[D9no5]");
        assertThat(transposer.transpose("[Cm7*]", 2, false)).isEqualTo("[Dm7*]");
    }

    @Test
    void malformedQuality_typo_leftUntouched() {
        // A real typo found in the catalog ("mjaj7" instead of "maj7") -
        // better to leave it visibly untransposed than silently guess wrong.
        assertThat(transposer.transpose("[Fmjaj7]word", 2, false)).isEqualTo("[Fmjaj7]word");
    }

    @Test
    void enharmonicSpellingsAreNormalized() {
        assertThat(transposer.transpose("[E#]weird", 0, false)).isEqualTo("[F]weird");
        assertThat(transposer.transpose("[Cb]weird", 0, false)).isEqualTo("[B]weird");
    }

    @Test
    void wrapsAroundNegativeHalfSteps() {
        assertThat(transposer.transpose("[C]", -1, false)).isEqualTo("[B]");
    }

    @Test
    void nullOrEmptyLine_returnsUnchanged() {
        assertThat(transposer.transpose(null, 2, false)).isNull();
        assertThat(transposer.transpose("", 2, false)).isEmpty();
    }

    @Test
    void transposeUpConvenienceMethod_usesSharps() {
        assertThat(transposer.transposeUp("[F]", 1)).isEqualTo("[F#]");
    }

    @Test
    void transposeDownConvenienceMethod_usesFlats() {
        assertThat(transposer.transposeDown("[D]", 1)).isEqualTo("[Db]");
    }

    // --- Key-driven spelling overload (MusicalKey, not a bare boolean) ---

    @Test
    void keyDrivenOverload_sharpKeyProducesSharpSpelling() {
        // Transposing into D major (sharp-preference key) should render F#, not Gb.
        assertThat(transposer.transpose("[F]riff", 1, MusicalKey.parse("D")))
                .isEqualTo("[F#]riff");
    }

    @Test
    void keyDrivenOverload_flatKeyProducesFlatSpelling() {
        // Transposing into Bb major (flat-preference key) should render Eb, not D#.
        assertThat(transposer.transpose("[D]riff", 1, MusicalKey.parse("Bb")))
                .isEqualTo("[Eb]riff");
    }

    @Test
    void keyDrivenOverload_neutralKeyDefaultsToSharps() {
        assertThat(transposer.transpose("[F]riff", 1, MusicalKey.parse("C")))
                .isEqualTo("[F#]riff");
    }

    // --- findUnrecognizedChordAttempts (Phase 1 warning guardrail) ---

    @Test
    void findUnrecognizedChordAttempts_flagsMalformedBracket() {
        assertThat(transposer.findUnrecognizedChordAttempts("[Fmjaj7]word"))
                .containsExactly("[Fmjaj7]");
    }

    @Test
    void findUnrecognizedChordAttempts_ignoresValidChords() {
        assertThat(transposer.findUnrecognizedChordAttempts("[C]hello [Gm7]world")).isEmpty();
    }

    @Test
    void findUnrecognizedChordAttempts_ignoresLegitimateNonChordBrackets() {
        // Section labels and riff notation are legitimate, not "unrecognized
        // chord attempts" - the guardrail must not cry wolf on these.
        assertThat(transposer.findUnrecognizedChordAttempts("[Bridge]")).isEmpty();
        assertThat(transposer.findUnrecognizedChordAttempts("[Bridge:]")).isEmpty();
        assertThat(transposer.findUnrecognizedChordAttempts("[Chorus]")).isEmpty();
        assertThat(transposer.findUnrecognizedChordAttempts("[bass]")).isEmpty();
        assertThat(transposer.findUnrecognizedChordAttempts("[E F# G A]")).isEmpty();
    }

    @Test
    void findUnrecognizedChordAttempts_ignoresFretPositionHints() {
        // Real notation from RunningOnEmpty-g.cho - a chord followed by a
        // guitar fret-position hint, not a typo.
        assertThat(transposer.findUnrecognizedChordAttempts("[C (17th - 3-6)]")).isEmpty();
        assertThat(transposer.findUnrecognizedChordAttempts("[Em (9th - 1,3)]")).isEmpty();
    }

    @Test
    void findUnrecognizedChordAttempts_findsMultiplePerLine() {
        assertThat(transposer.findUnrecognizedChordAttempts("[Fmjaj7]word[Gdimzz]"))
                .containsExactly("[Fmjaj7]", "[Gdimzz]");
    }

    @Test
    void findUnrecognizedChordAttempts_nullOrEmptyLine_returnsEmpty() {
        assertThat(transposer.findUnrecognizedChordAttempts(null)).isEmpty();
        assertThat(transposer.findUnrecognizedChordAttempts("")).isEmpty();
    }

    // --- extractChordRoots (Phase 2 harmonic drift check) ---

    @Test
    void extractChordRoots_returnsRootsInOrder() {
        assertThat(transposer.extractChordRoots("[C]Hello [Am]world [F]this [G]is a test"))
                .containsExactly("C", "A", "F", "G");
    }

    @Test
    void extractChordRoots_ignoresNonChordBrackets() {
        assertThat(transposer.extractChordRoots("[C][Bridge][Am][E F# G A]"))
                .containsExactly("C", "A");
    }

    @Test
    void extractChordRoots_slashChordReturnsRootOnly() {
        assertThat(transposer.extractChordRoots("[A/E]riff")).containsExactly("A");
    }

    @Test
    void extractChordRoots_nullOrEmptyLine_returnsEmpty() {
        assertThat(transposer.extractChordRoots(null)).isEmpty();
        assertThat(transposer.extractChordRoots("")).isEmpty();
    }

    @Test
    void extractChordRoots_noChords_returnsEmpty() {
        assertThat(transposer.extractChordRoots("just plain lyrics, no chords here")).isEmpty();
    }

    // --- bracketBareChords (instrumental-notation normalization) ---

    @Test
    void bracketBareChords_wrapsSimpleBareChords() {
        assertThat(transposer.bracketBareChords("| C   . . .  | C   . . . |").getLine())
                .isEqualTo("| [C]   . . .  | [C]   . . . |");
    }

    @Test
    void bracketBareChords_wrapsQualityAndSlashBass() {
        assertThat(transposer.bracketBareChords("| Am7 . . . | G/B . . . |").getLine())
                .isEqualTo("| [Am7] . . . | [G/B] . . . |");
    }

    @Test
    void bracketBareChords_reportsWrappedTokens() {
        assertThat(transposer.bracketBareChords("| C . . . | Am7 . . . |").getWrappedTokens())
                .containsExactly("C", "Am7");
    }

    @Test
    void bracketBareChords_leavesAlreadyBracketedChordsUntouched_noDoubleWrap() {
        assertThat(transposer.bracketBareChords("[Am7/D] . . . | [G]|").getLine())
                .isEqualTo("[Am7/D] . . . | [G]|");
        assertThat(transposer.bracketBareChords("[Am7/D] . . . | [G]|").changed()).isFalse();
    }

    @Test
    void bracketBareChords_leavesNoChordMarkerUntouched() {
        assertThat(transposer.bracketBareChords("| N.C. | Gm |").getLine())
                .isEqualTo("| N.C. | [Gm] |");
    }

    @Test
    void bracketBareChords_leavesDotsAndPipesUntouched() {
        assertThat(transposer.bracketBareChords(". | . . . |").getLine())
                .isEqualTo(". | . . . |");
    }

    @Test
    void bracketBareChords_leavesRepeatShorthandAndStrumSlashUntouched() {
        // Repeat/strum notation is a separate, un-fixed drift category -
        // this method's job is only bracket-wrapping actual chords.
        assertThat(transposer.bracketBareChords("C | G | A | D :|| x 3").getLine())
                .isEqualTo("[C] | [G] | [A] | [D] :|| x 3");
        assertThat(transposer.bracketBareChords("| / / / / |").getLine())
                .isEqualTo("| / / / / |");
    }

    @Test
    void bracketBareChords_doesNotMatchLetterInsideOrdinaryWord() {
        // Regression: an early version of the word-boundary regex matched
        // the bare "f" inside "if" as a chord attempt.
        assertThat(transposer.bracketBareChords("| G . (if you believe)").getLine())
                .isEqualTo("| [G] . (if you believe)");
    }

    @Test
    void bracketBareChords_leavesSectionLabelsUntouched() {
        assertThat(transposer.bracketBareChords("{start_of_part: Bridge}").getLine())
                .isEqualTo("{start_of_part: Bridge}");
    }

    @Test
    void bracketBareChords_noBareChords_reportsUnchanged() {
        BracketedLine result = transposer.bracketBareChords("[C] . . . | [G] . . . |");
        assertThat(result.changed()).isFalse();
        assertThat(result.getWrappedTokens()).isEmpty();
    }

    @Test
    void bracketBareChords_nullOrEmptyLine_returnsUnchanged() {
        assertThat(transposer.bracketBareChords(null).getLine()).isEmpty();
        assertThat(transposer.bracketBareChords("").getLine()).isEmpty();
    }
}
