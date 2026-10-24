package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import com.pourchoices.chordpro.application.domain.model.GuitarSwitch;
import com.pourchoices.chordpro.application.domain.model.GuitarType;
import com.pourchoices.chordpro.application.domain.model.SetlistAssignment;
import com.pourchoices.chordpro.application.domain.model.SetlistEntry;
import com.pourchoices.chordpro.application.domain.model.SongId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link GuitarSwitchScorer} — pure logic, no I/O, exercised
 * directly with synthetic {@link SetlistEntry} lists. Mirrors the style of
 * {@link SetlistArcScorerTest}.
 */
class GuitarSwitchScorerTest {

    private final GuitarSwitchScorer scorer = new GuitarSwitchScorer();
    private int counter = 0;

    private SetlistEntry entry(String set, GuitarType guitarType) {
        counter++;
        CatalogEntry song = CatalogEntry.builder()
                .songId(SongId.parse("ABC:B:TestArtist:Song" + counter))
                .title("Song " + counter)
                .artist("Test Artist")
                .key("C")
                .duration("3:00")
                .guitarType(guitarType)
                .build();
        SetlistAssignment assignment = SetlistAssignment.builder()
                .gig("2026-01-01-test")
                .songId(song.getSongId())
                .set(set)
                .build();
        return SetlistEntry.builder().song(song).assignment(assignment).build();
    }

    @Test
    void emptyList_returnsNoSwitches() {
        assertThat(scorer.detectSwitches(List.of())).isEmpty();
    }

    @Test
    void singleSong_returnsNoSwitches() {
        assertThat(scorer.detectSwitches(List.of(entry("A01", GuitarType.ACOUSTIC)))).isEmpty();
    }

    @Test
    void sameGuitarTypeBackToBack_isNotASwitch() {
        List<SetlistEntry> entries = List.of(
                entry("A01", GuitarType.ELECTRIC),
                entry("A02", GuitarType.ELECTRIC));

        assertThat(scorer.detectSwitches(entries)).isEmpty();
    }

    @Test
    void differentGuitarTypeBackToBack_isASwitch() {
        List<SetlistEntry> entries = List.of(
                entry("A01", GuitarType.ACOUSTIC),
                entry("A02", GuitarType.BASS));

        List<GuitarSwitch> switches = scorer.detectSwitches(entries);

        assertThat(switches).hasSize(1);
        GuitarSwitch s = switches.get(0);
        assertThat(s.getSet()).isEqualTo("A02");
        assertThat(s.getFromGuitar()).isEqualTo(GuitarType.ACOUSTIC);
        assertThat(s.getToGuitar()).isEqualTo(GuitarType.BASS);
    }

    @Test
    void threeConsecutiveDifferentTypes_detectsTwoSwitches() {
        List<SetlistEntry> entries = List.of(
                entry("A01", GuitarType.ACOUSTIC),
                entry("A02", GuitarType.ELECTRIC),
                entry("A03", GuitarType.BASS));

        assertThat(scorer.detectSwitches(entries)).hasSize(2);
    }

    @Test
    void nullGuitarType_isNeverASwitch() {
        List<SetlistEntry> entries = List.of(
                entry("A01", GuitarType.ACOUSTIC),
                entry("A02", null),
                entry("A03", GuitarType.ELECTRIC));

        assertThat(scorer.detectSwitches(entries)).isEmpty();
    }

    @Test
    void explicitUnknownGuitarType_isNeverASwitch() {
        List<SetlistEntry> entries = List.of(
                entry("A01", GuitarType.ACOUSTIC),
                entry("A02", GuitarType.UNKNOWN),
                entry("A03", GuitarType.ELECTRIC));

        assertThat(scorer.detectSwitches(entries)).isEmpty();
    }
}
