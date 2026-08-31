package com.pourchoices.chordpro.application.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link SongId}. Had zero direct coverage before — every
 * other test exercised it only indirectly through a service.
 */
class SongIdTest {

    // ── parse ────────────────────────────────────────────────────────────

    @Test
    void parse_baseVersion_noKeyAlternative() {
        SongId songId = SongId.parse("ABC:B:BillyJoel:PianoMan");

        assertThat(songId.getClusterPrefix()).isEqualTo("ABC");
        assertThat(songId.getClusterElement()).isEqualTo("B");
        assertThat(songId.getArtist()).isEqualTo("BillyJoel");
        assertThat(songId.getTitle()).isEqualTo("PianoMan");
        assertThat(songId.getKeyAlternative()).isNull();
        assertThat(songId.isBaseVersion()).isTrue();
        assertThat(songId.toString()).isEqualTo("ABC:B:BillyJoel:PianoMan");
    }

    @Test
    void parse_keyVariant_extractsSuffix() {
        SongId songId = SongId.parse("ABC:B:BillyJoel:PianoMan-a");

        assertThat(songId.getTitle()).isEqualTo("PianoMan");
        assertThat(songId.getKeyAlternative()).isEqualTo("a");
        assertThat(songId.isBaseVersion()).isFalse();
        assertThat(songId.toString()).isEqualTo("ABC:B:BillyJoel:PianoMan-a");
    }

    @Test
    void parse_minorKeyVariant_extractsSuffixWithM() {
        SongId songId = SongId.parse("ABC:B:BobSeger:HollywoodNights-g#m");

        assertThat(songId.getTitle()).isEqualTo("HollywoodNights");
        assertThat(songId.getKeyAlternative()).isEqualTo("g#m");
    }

    @Test
    void parse_nonKeySuffix_isNotTreatedAsKeyAlternative() {
        // "-old" doesn't match the key-suffix pattern, so it stays part of the title.
        SongId songId = SongId.parse("ABC:B:BillyJoel:PianoMan-old");

        assertThat(songId.getTitle()).isEqualTo("PianoMan-old");
        assertThat(songId.getKeyAlternative()).isNull();
    }

    @Test
    void parse_wrongSegmentCount_throws() {
        assertThatThrownBy(() -> SongId.parse("ABC:B:BillyJoel"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("4 colon-separated segments");
    }

    @Test
    void parse_invalidClusterPrefix_throws() {
        assertThatThrownBy(() -> SongId.parse("abc:B:BillyJoel:PianoMan"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("clusterPrefix must be 2-3 uppercase letters");
    }

    @Test
    void parse_blank_throws() {
        assertThatThrownBy(() -> SongId.parse(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── toGroupKey ───────────────────────────────────────────────────────

    @Test
    void toGroupKey_stripsKeyAlternative() {
        SongId base = SongId.parse("ABC:B:BillyJoel:PianoMan");
        SongId variant = SongId.parse("ABC:B:BillyJoel:PianoMan-a");

        assertThat(base.toGroupKey()).isEqualTo("ABC:B:BillyJoel:PianoMan");
        assertThat(variant.toGroupKey()).isEqualTo("ABC:B:BillyJoel:PianoMan");
        assertThat(base.toGroupKey()).isEqualTo(variant.toGroupKey());
    }

    // ── withKeyAlternative ───────────────────────────────────────────────

    @Test
    void withKeyAlternative_fromBase_derivesVariant() {
        SongId base = SongId.parse("ABC:B:BillyJoel:PianoMan");

        SongId variant = base.withKeyAlternative("bb");

        assertThat(variant.toString()).isEqualTo("ABC:B:BillyJoel:PianoMan-bb");
        assertThat(variant.getClusterPrefix()).isEqualTo(base.getClusterPrefix());
        assertThat(variant.getClusterElement()).isEqualTo(base.getClusterElement());
        assertThat(variant.getArtist()).isEqualTo(base.getArtist());
        assertThat(variant.getTitle()).isEqualTo(base.getTitle());
        assertThat(variant.isBaseVersion()).isFalse();
    }

    @Test
    void withKeyAlternative_fromOneVariant_derivesAnother_sameGroup() {
        SongId variantA = SongId.parse("ABC:B:BobSeger:HollywoodNights-b");

        SongId variantE = variantA.withKeyAlternative("e");

        assertThat(variantE.toString()).isEqualTo("ABC:B:BobSeger:HollywoodNights-e");
        assertThat(variantE.toGroupKey()).isEqualTo(variantA.toGroupKey());
    }

    @Test
    void withKeyAlternative_null_derivesBaseVersion() {
        SongId variant = SongId.parse("ABC:B:BillyJoel:PianoMan-a");

        SongId base = variant.withKeyAlternative(null);

        assertThat(base.toString()).isEqualTo("ABC:B:BillyJoel:PianoMan");
        assertThat(base.isBaseVersion()).isTrue();
    }

    @Test
    void withKeyAlternative_doesNotMutateOriginal() {
        SongId original = SongId.parse("ABC:B:BillyJoel:PianoMan");

        original.withKeyAlternative("a");

        assertThat(original.isBaseVersion()).isTrue();
        assertThat(original.toString()).isEqualTo("ABC:B:BillyJoel:PianoMan");
    }
}
