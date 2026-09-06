package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.ArcThird;
import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import com.pourchoices.chordpro.application.domain.model.SetlistAssignment;
import com.pourchoices.chordpro.application.domain.model.SetlistEntry;
import com.pourchoices.chordpro.application.domain.model.SongId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SetlistArcScorer} — pure logic, no I/O, exercised
 * directly with synthetic {@link SetlistEntry} lists.
 */
class SetlistArcScorerTest {

    private final SetlistArcScorer scorer = new SetlistArcScorer();
    private int counter = 0;

    private SetlistEntry entry(String set, Integer energyLevel, Boolean singAlong) {
        counter++;
        CatalogEntry song = CatalogEntry.builder()
                .songId(SongId.parse("ABC:B:TestArtist:Song" + counter))
                .title("Song " + counter)
                .artist("Test Artist")
                .key("C")
                .duration("3:00")
                .energyLevel(energyLevel)
                .singAlong(singAlong)
                .build();
        SetlistAssignment assignment = SetlistAssignment.builder()
                .gig("2026-01-01-test")
                .songId(song.getSongId())
                .set(set)
                .build();
        return SetlistEntry.builder().song(song).assignment(assignment).build();
    }

    @Test
    void computeThirds_emptyList_returnsEmpty() {
        assertThat(scorer.computeThirds(List.of())).isEmpty();
    }

    @Test
    void computeThirds_ninesongs_splitsEvenly() {
        List<SetlistEntry> entries = List.of(
                entry("A01", 1, null), entry("A02", 2, null), entry("A03", 3, null),
                entry("B01", 5, null), entry("B02", 6, null), entry("B03", 7, null),
                entry("C01", 8, null), entry("C02", 9, null), entry("C03", 10, null)
        );

        List<ArcThird> thirds = scorer.computeThirds(entries);

        assertThat(thirds).hasSize(3);
        assertThat(thirds.get(0).getLabel()).isEqualTo("Opening");
        assertThat(thirds.get(0).getSongCount()).isEqualTo(3);
        assertThat(thirds.get(0).getAvgEnergy()).isEqualTo(2.0);
        assertThat(thirds.get(2).getLabel()).isEqualTo("Final");
        assertThat(thirds.get(2).getAvgEnergy()).isEqualTo(9.0);
    }

    @Test
    void computeThirds_remainderGoesToFinalThird() {
        // 10 songs: 3/3/4 split (n/3 = 3, remainder to the final third)
        List<SetlistEntry> entries = List.of(
                entry("A01", 1, null), entry("A02", 1, null), entry("A03", 1, null),
                entry("B01", 5, null), entry("B02", 5, null), entry("B03", 5, null),
                entry("C01", 9, null), entry("C02", 9, null), entry("C03", 9, null), entry("C04", 9, null)
        );

        List<ArcThird> thirds = scorer.computeThirds(entries);

        assertThat(thirds.get(0).getSongCount()).isEqualTo(3);
        assertThat(thirds.get(1).getSongCount()).isEqualTo(3);
        assertThat(thirds.get(2).getSongCount()).isEqualTo(4);
    }

    @Test
    void computeThirds_nullEnergyLevels_excludedFromAverageButCounted() {
        // 9 songs -> thirdSize=3, so the opening third gets exactly indices 0-2.
        List<SetlistEntry> entries = List.of(
                entry("A01", null, null), entry("A02", null, null), entry("A03", 4, null),
                entry("B01", 5, null), entry("B02", 5, null), entry("B03", 5, null),
                entry("C01", 5, null), entry("C02", 5, null), entry("C03", 5, null)
        );

        List<ArcThird> thirds = scorer.computeThirds(entries);

        ArcThird opening = thirds.get(0);
        assertThat(opening.getSongCount()).isEqualTo(3);
        assertThat(opening.getCharacterizedCount()).isEqualTo(1);
        assertThat(opening.getAvgEnergy()).isEqualTo(4.0);
    }

    @Test
    void computeThirds_allNullEnergy_avgAndMedianAreNull() {
        List<SetlistEntry> entries = List.of(entry("A01", null, null), entry("A02", null, null), entry("A03", null, null));

        ArcThird opening = scorer.computeThirds(entries).get(0);

        assertThat(opening.getCharacterizedCount()).isZero();
        assertThat(opening.getAvgEnergy()).isNull();
        assertThat(opening.getMedianEnergy()).isNull();
    }

    @Test
    void computeThirds_medianEvenCount_isAverageOfMiddleTwo() {
        List<SetlistEntry> entries = List.of(entry("A01", 2, null), entry("A02", 4, null), entry("A03", 6, null), entry("A04", 8, null));
        // n=4 -> thirdSize=1, thirds are [0:1), [1:2), [2:4) -> opening has 1 song, middle 1, final 2
        ArcThird finalThird = scorer.computeThirds(entries).get(2);

        assertThat(finalThird.getSongCount()).isEqualTo(2);
        assertThat(finalThird.getMedianEnergy()).isEqualTo(7.0); // (6+8)/2
    }

    @Test
    void singAlongCount_countsOnlyTrueFlags() {
        List<SetlistEntry> entries = List.of(
                entry("A01", 5, true), entry("A02", 5, false), entry("A03", 5, null), entry("A04", 5, true)
        );

        assertThat(scorer.singAlongCount(entries)).isEqualTo(2);
    }

    @Test
    void singAlongCountInFinalThird_onlyCountsFinalThird() {
        List<SetlistEntry> entries = List.of(
                entry("A01", 5, true), entry("A02", 5, true), entry("A03", 5, true), // opening
                entry("B01", 5, false), entry("B02", 5, false), entry("B03", 5, false), // middle
                entry("C01", 5, true), entry("C02", 5, false), entry("C03", 5, false) // final
        );

        assertThat(scorer.singAlongCountInFinalThird(entries)).isEqualTo(1);
    }

    @Test
    void lastSongsIncludeSingAlong_true_whenOneOfLastWindowIsSingAlong() {
        List<SetlistEntry> entries = List.of(
                entry("A01", 5, false), entry("A02", 5, false),
                entry("A03", 5, false), entry("A04", 5, true), entry("A05", 5, false)
        );

        assertThat(scorer.lastSongsIncludeSingAlong(entries, 3)).isTrue();
    }

    @Test
    void lastSongsIncludeSingAlong_false_whenNoneOfLastWindowIsSingAlong() {
        List<SetlistEntry> entries = List.of(
                entry("A01", 5, true), entry("A02", 5, false), entry("A03", 5, false)
        );

        assertThat(scorer.lastSongsIncludeSingAlong(entries, 2)).isFalse();
    }

    @Test
    void lastSongsIncludeSingAlong_emptyList_isFalse() {
        assertThat(scorer.lastSongsIncludeSingAlong(List.of(), 3)).isFalse();
    }

    @Test
    void windowLargerThanList_doesNotThrow() {
        List<SetlistEntry> entries = List.of(entry("A01", 5, true));

        assertThat(scorer.lastSongsIncludeSingAlong(entries, 10)).isTrue();
    }
}
