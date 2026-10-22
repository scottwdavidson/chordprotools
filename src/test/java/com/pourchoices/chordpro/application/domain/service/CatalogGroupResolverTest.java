package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import com.pourchoices.chordpro.application.domain.model.SongId;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link CatalogGroupResolver} — the shared "find all
 * key-variants of this song" lookup used by {@code update-song},
 * {@code consistent-song-data}, and {@code assign-backing-track-slots}.
 */
class CatalogGroupResolverTest {

    private final CatalogGroupResolver resolver = new CatalogGroupResolver();

    private static CatalogEntry entry(String songId) {
        return CatalogEntry.builder()
                .songId(SongId.parse(songId))
                .title("Piano Man")
                .artist("Billy Joel")
                .key("C")
                .duration("4:30")
                .build();
    }

    private static Map<String, CatalogEntry> catalogOf(CatalogEntry... entries) {
        Map<String, CatalogEntry> map = new LinkedHashMap<>();
        for (CatalogEntry e : entries) {
            map.put(e.getSongId().toString(), e);
        }
        return map;
    }

    @Test
    void findGroupVariants_returnsBaseAndAllKeyVariants() {
        CatalogEntry base = entry("ABC:B:BillyJoel:PianoMan");
        CatalogEntry variantA = entry("ABC:B:BillyJoel:PianoMan-a");
        CatalogEntry variantC = entry("ABC:B:BillyJoel:PianoMan-c");
        CatalogEntry otherSong = entry("DEF:E:EltonJohn:Daniel");

        Map<String, CatalogEntry> catalog = catalogOf(base, variantA, variantC, otherSong);

        List<CatalogEntry> variants = resolver.findGroupVariants(SongId.parse("ABC:B:BillyJoel:PianoMan"), catalog);

        assertThat(variants).containsExactlyInAnyOrder(base, variantA, variantC);
    }

    @Test
    void findGroupVariants_canBeAskedFromAnyVariant_sameGroupResult() {
        CatalogEntry base = entry("ABC:B:BillyJoel:PianoMan");
        CatalogEntry variantA = entry("ABC:B:BillyJoel:PianoMan-a");

        Map<String, CatalogEntry> catalog = catalogOf(base, variantA);

        List<CatalogEntry> variants = resolver.findGroupVariants(SongId.parse("ABC:B:BillyJoel:PianoMan-a"), catalog);

        assertThat(variants).containsExactlyInAnyOrder(base, variantA);
    }

    @Test
    void findGroupVariants_noVariants_returnsJustTheOneEntry() {
        CatalogEntry base = entry("ABC:B:BillyJoel:PianoMan");
        Map<String, CatalogEntry> catalog = catalogOf(base);

        List<CatalogEntry> variants = resolver.findGroupVariants(SongId.parse("ABC:B:BillyJoel:PianoMan"), catalog);

        assertThat(variants).containsExactly(base);
    }

    @Test
    void findGroupVariants_unknownSongId_returnsEmpty() {
        Map<String, CatalogEntry> catalog = catalogOf(entry("ABC:B:BillyJoel:PianoMan"));

        List<CatalogEntry> variants = resolver.findGroupVariants(SongId.parse("DEF:E:EltonJohn:Daniel"), catalog);

        assertThat(variants).isEmpty();
    }
}
