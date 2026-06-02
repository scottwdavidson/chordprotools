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
}
