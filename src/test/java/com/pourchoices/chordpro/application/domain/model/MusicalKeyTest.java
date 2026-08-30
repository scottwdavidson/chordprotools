package com.pourchoices.chordpro.application.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link MusicalKey} — parsing, major/minor, and (crucially)
 * enharmonic equality.
 */
class MusicalKeyTest {

    @Test
    void parsesMajorKey() {
        MusicalKey c = MusicalKey.parse("C");
        assertThat(c.getChromaticPosition()).isZero();
        assertThat(c.isMinor()).isFalse();
    }

    @Test
    void parsesMinorKey_lowercaseAndUppercase() {
        assertThat(MusicalKey.parse("am")).isEqualTo(MusicalKey.parse("Am"));
        assertThat(MusicalKey.parse("Am").isMinor()).isTrue();
    }

    @Test
    void enharmonicsAreEqual_sharpEqualsFlat() {
        assertThat(MusicalKey.parse("A#")).isEqualTo(MusicalKey.parse("Bb"));
        assertThat(MusicalKey.parse("C#m")).isEqualTo(MusicalKey.parse("Dbm"));
    }

    @Test
    void majorAndMinorAreNotEqual() {
        assertThat(MusicalKey.parse("C")).isNotEqualTo(MusicalKey.parse("Cm"));
    }

    @Test
    void wrapsAroundChromatically() {
        // B# == C, Cb == B
        assertThat(MusicalKey.parse("B#")).isEqualTo(MusicalKey.parse("C"));
        assertThat(MusicalKey.parse("Cb")).isEqualTo(MusicalKey.parse("B"));
    }

    @Test
    void caseInsensitiveRoot() {
        assertThat(MusicalKey.parse("g")).isEqualTo(MusicalKey.parse("G"));
    }

    @Test
    void isParseable_guardsBadInput() {
        assertThat(MusicalKey.isParseable("C")).isTrue();
        assertThat(MusicalKey.isParseable("Bb")).isTrue();
        assertThat(MusicalKey.isParseable("H")).isFalse();
        assertThat(MusicalKey.isParseable("")).isFalse();
        assertThat(MusicalKey.isParseable(null)).isFalse();
        assertThat(MusicalKey.isParseable("C#7")).isFalse(); // not a bare key
    }

    @Test
    void parseRejectsBlankAndGarbage() {
        assertThatThrownBy(() -> MusicalKey.parse(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MusicalKey.parse("nope"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void prefersFlats_flatMajorKeys() {
        assertThat(MusicalKey.parse("F").prefersFlats()).isTrue();
        assertThat(MusicalKey.parse("Bb").prefersFlats()).isTrue();
        assertThat(MusicalKey.parse("Eb").prefersFlats()).isTrue();
        assertThat(MusicalKey.parse("Ab").prefersFlats()).isTrue();
        assertThat(MusicalKey.parse("Db").prefersFlats()).isTrue();
    }

    @Test
    void prefersFlats_flatMinorKeys() {
        assertThat(MusicalKey.parse("Dm").prefersFlats()).isTrue();
        assertThat(MusicalKey.parse("Gm").prefersFlats()).isTrue();
        assertThat(MusicalKey.parse("Cm").prefersFlats()).isTrue();
        assertThat(MusicalKey.parse("Fm").prefersFlats()).isTrue();
        assertThat(MusicalKey.parse("Bbm").prefersFlats()).isTrue();
        assertThat(MusicalKey.parse("Ebm").prefersFlats()).isTrue();
    }

    @Test
    void prefersFlats_sharpMajorKeys() {
        assertThat(MusicalKey.parse("G").prefersFlats()).isFalse();
        assertThat(MusicalKey.parse("D").prefersFlats()).isFalse();
        assertThat(MusicalKey.parse("A").prefersFlats()).isFalse();
        assertThat(MusicalKey.parse("E").prefersFlats()).isFalse();
        assertThat(MusicalKey.parse("B").prefersFlats()).isFalse();
    }

    @Test
    void prefersFlats_sharpMinorKeys() {
        assertThat(MusicalKey.parse("Em").prefersFlats()).isFalse();
        assertThat(MusicalKey.parse("Bm").prefersFlats()).isFalse();
        assertThat(MusicalKey.parse("C#m").prefersFlats()).isFalse();
        assertThat(MusicalKey.parse("G#m").prefersFlats()).isFalse();
    }

    @Test
    void prefersFlats_neutralKeysDefaultToSharp() {
        assertThat(MusicalKey.parse("C").prefersFlats()).isFalse();
        assertThat(MusicalKey.parse("Am").prefersFlats()).isFalse();
    }

    @Test
    void prefersFlats_documentedTieBreaks() {
        // F#/Gb major: both valid enharmonic spellings of the same key.
        // Deliberately defaults to sharp (F#) - more common in this band's
        // rock/pop repertoire. See MusicalKey.FLAT_MAJOR_POSITIONS javadoc.
        assertThat(MusicalKey.parse("F#").prefersFlats()).isFalse();

        // D#m/Ebm minor: both valid enharmonic spellings of the same key.
        // Deliberately defaults to flat (Ebm) - D# minor is essentially
        // never used in real charts. See FLAT_MINOR_POSITIONS javadoc.
        assertThat(MusicalKey.parse("D#m").prefersFlats()).isTrue();
    }

    @Test
    void noteName_sharpsAndFlats() {
        assertThat(MusicalKey.noteName(6, false)).isEqualTo("F#");
        assertThat(MusicalKey.noteName(6, true)).isEqualTo("Gb");
        assertThat(MusicalKey.noteName(0, false)).isEqualTo("C");
    }

    @Test
    void noteName_normalizesOutOfRangePositions() {
        assertThat(MusicalKey.noteName(-1, false)).isEqualTo("B");
        assertThat(MusicalKey.noteName(12, false)).isEqualTo("C");
    }

    @Test
    void canonicalName_majorAndMinor() {
        assertThat(MusicalKey.parse("C").canonicalName()).isEqualTo("C");
        assertThat(MusicalKey.parse("Am").canonicalName()).isEqualTo("Am");
    }

    @Test
    void canonicalName_usesKeyDrivenSpelling() {
        // Bb major prefers flats - a key that's chromatically Bb should
        // always render as "Bb", never "A#", regardless of input spelling.
        assertThat(MusicalKey.parse("A#").canonicalName()).isEqualTo("Bb");
        // D major prefers sharps - chromatically F# should render as "F#".
        assertThat(MusicalKey.parse("Gb").canonicalName()).isEqualTo("F#");
    }

    @Test
    void transposeBy_preservesQuality() {
        assertThat(MusicalKey.parse("C").transposeBy(2)).isEqualTo(MusicalKey.parse("D"));
        assertThat(MusicalKey.parse("Am").transposeBy(2)).isEqualTo(MusicalKey.parse("Bm"));
    }

    @Test
    void transposeBy_wrapsAroundNegativeAndOverflow() {
        assertThat(MusicalKey.parse("C").transposeBy(-1)).isEqualTo(MusicalKey.parse("B"));
        assertThat(MusicalKey.parse("B").transposeBy(1)).isEqualTo(MusicalKey.parse("C"));
        assertThat(MusicalKey.parse("C").transposeBy(13)).isEqualTo(MusicalKey.parse("C#"));
    }

    @Test
    void transposeBy_zeroReturnsSameKey() {
        assertThat(MusicalKey.parse("F#m").transposeBy(0)).isEqualTo(MusicalKey.parse("F#m"));
    }
}
