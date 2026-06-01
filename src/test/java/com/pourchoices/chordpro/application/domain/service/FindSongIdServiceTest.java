package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import com.pourchoices.chordpro.application.domain.model.SongId;
import com.pourchoices.chordpro.application.domain.model.SongMatch;
import com.pourchoices.chordpro.application.port.out.CatalogPort;
import com.pourchoices.chordpro.config.ChordproCatalogIndexPathConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for FindSongIdService — fragment matching, key-variant grouping,
 * variant counting, orphan handling, and sorting. These tests exercise the
 * logic that previously lived (untested) in the find-song-id Python script.
 */
class FindSongIdServiceTest {

    private CatalogPort catalogPort;
    private FindSongIdService service;

    @BeforeEach
    void setUp() {
        catalogPort = mock(CatalogPort.class);
        ChordproCatalogIndexPathConfig config = mock(ChordproCatalogIndexPathConfig.class);
        when(config.getCatalogIndexPath()).thenReturn("./song-catalog.csv");
        service = new FindSongIdService(catalogPort, config);
    }

    private static CatalogEntry entry(String songId, String title, String artist, String key) {
        return CatalogEntry.builder()
                .songId(SongId.parse(songId))
                .title(title)
                .artist(artist)
                .key(key)
                .duration("3:00")
                .build();
    }

    private void givenCatalog(CatalogEntry... entries) {
        // LinkedHashMap to keep deterministic insertion order for the test
        Map<String, CatalogEntry> catalog = new LinkedHashMap<>();
        for (CatalogEntry e : entries) {
            catalog.put(e.getSongId().toString(), e);
        }
        when(catalogPort.readCatalogFromCsv(any())).thenReturn(catalog);
    }

    // ── Fragment matching ─────────────────────────────────────────────────

    @Test
    void matchesByTitleFragment_caseInsensitive() {
        givenCatalog(entry("ABC:B:BillyJoel:PianoMan", "Piano Man", "Billy Joel", "C"));

        List<SongMatch> result = service.findByFragment("piano");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Piano Man");
        assertThat(result.get(0).getDisplaySongId()).isEqualTo("ABC:B:BillyJoel:PianoMan");
    }

    @Test
    void matchesByArtistFragment_caseInsensitive() {
        givenCatalog(
                entry("ABC:B:BillyJoel:PianoMan", "Piano Man", "Billy Joel", "C"),
                entry("DEF:E:EltonJohn:Daniel",   "Daniel",    "Elton John", "C")
        );

        List<SongMatch> result = service.findByFragment("JOEL");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getArtist()).isEqualTo("Billy Joel");
    }

    @Test
    void noMatch_returnsEmptyList() {
        givenCatalog(entry("ABC:B:BillyJoel:PianoMan", "Piano Man", "Billy Joel", "C"));
        assertThat(service.findByFragment("zeppelin")).isEmpty();
    }

    // ── Key-variant grouping ──────────────────────────────────────────────

    @Test
    void baseAndVariants_collapseToOneRow_withVariantCount() {
        givenCatalog(
                entry("ABC:B:BillyJoel:MyLife",   "My Life", "Billy Joel", "Eb"),
                entry("ABC:B:BillyJoel:MyLife-c", "My Life", "Billy Joel", "C"),
                entry("ABC:B:BillyJoel:MyLife-d", "My Life", "Billy Joel", "D")
        );

        List<SongMatch> result = service.findByFragment("my life");

        assertThat(result).hasSize(1);
        SongMatch m = result.get(0);
        assertThat(m.getDisplaySongId()).isEqualTo("ABC:B:BillyJoel:MyLife"); // base/group key
        assertThat(m.getVariantCount()).isEqualTo(2);
        assertThat(m.isOrphan()).isFalse();
        assertThat(m.getKey()).isEqualTo("Eb"); // representative is the base version
    }

    @Test
    void noVariants_variantCountZero() {
        givenCatalog(entry("DEF:E:EltonJohn:Daniel", "Daniel", "Elton John", "C"));

        List<SongMatch> result = service.findByFragment("daniel");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getVariantCount()).isZero();
    }

    // ── Orphan handling ───────────────────────────────────────────────────

    @Test
    void orphanVariant_noBase_isPromotedAndFlagged() {
        // Only a key variant exists — no base version in the catalog
        givenCatalog(entry("ABC:B:BillyJoel:MyLife-c", "My Life", "Billy Joel", "C"));

        List<SongMatch> result = service.findByFragment("my life");

        assertThat(result).hasSize(1);
        SongMatch m = result.get(0);
        assertThat(m.isOrphan()).isTrue();
        // Orphan shows its own suffixed ID, not the (nonexistent) base
        assertThat(m.getDisplaySongId()).isEqualTo("ABC:B:BillyJoel:MyLife-c");
        assertThat(m.getVariantCount()).isEqualTo(1);
    }

    // ── Sorting ───────────────────────────────────────────────────────────

    @Test
    void results_sortedByTitleCaseInsensitive() {
        givenCatalog(
                entry("VWX:W:Weezer:Buddyholly",  "buddy holly", "Weezer",     "A"),
                entry("ABC:A:Adele:Hello",        "Hello",       "Adele",      "Fm"),
                entry("ABC:A:Aha:TakeOnMe",       "Take On Me",  "a-ha",       "A")
        );

        // Fragment that matches all three (empty matches everything)
        List<SongMatch> result = service.findByFragment("");

        assertThat(result).extracting(SongMatch::getTitle)
                .containsExactly("buddy holly", "Hello", "Take On Me");
    }
}
