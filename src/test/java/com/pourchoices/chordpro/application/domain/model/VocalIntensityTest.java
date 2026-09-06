package com.pourchoices.chordpro.application.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link VocalIntensity#fromString(String)}.
 * Mirrors the tolerant-parsing contract of {@link BackingType#fromString(String)}.
 */
class VocalIntensityTest {

    @Test
    void parsesKnownValuesCaseInsensitively() {
        assertThat(VocalIntensity.fromString("NONE")).isEqualTo(VocalIntensity.NONE);
        assertThat(VocalIntensity.fromString("light")).isEqualTo(VocalIntensity.LIGHT);
        assertThat(VocalIntensity.fromString("Full")).isEqualTo(VocalIntensity.FULL);
    }

    @Test
    void returnsNullForBlankOrMissing() {
        assertThat(VocalIntensity.fromString(null)).isNull();
        assertThat(VocalIntensity.fromString("")).isNull();
        assertThat(VocalIntensity.fromString("   ")).isNull();
    }

    @Test
    void returnsNullForUnrecognisedValueRatherThanThrowing() {
        assertThat(VocalIntensity.fromString("LOUD")).isNull();
    }

    @Test
    void ordinalOrderingSupportsVenuePolicyComparison() {
        assertThat(VocalIntensity.NONE.ordinal()).isLessThan(VocalIntensity.LIGHT.ordinal());
        assertThat(VocalIntensity.LIGHT.ordinal()).isLessThan(VocalIntensity.FULL.ordinal());
    }
}
