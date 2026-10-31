package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import com.pourchoices.chordpro.application.domain.model.SetlistAssignment;
import com.pourchoices.chordpro.application.domain.model.SetlistEntry;
import com.pourchoices.chordpro.application.domain.model.SongId;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Unit tests for {@link SetlistPageSplitter} — pure logic, synthetic entries. */
class SetlistPageSplitterTest {

    private final SetlistPageSplitter splitter = new SetlistPageSplitter();
    private int counter = 0;

    private SetlistEntry entry(String set) {
        counter++;
        CatalogEntry song = CatalogEntry.builder()
                .songId(SongId.parse("ABC:B:TestArtist:Song" + counter))
                .title("Song " + counter).artist("Test Artist").key("C").duration("3:00").build();
        SetlistAssignment assignment = SetlistAssignment.builder()
                .gig("2026-01-01-test").songId(song.getSongId()).set(set).build();
        return SetlistEntry.builder().song(song).assignment(assignment).build();
    }

    /** e.g. entries("A", 3) → A01, A02, A03 */
    private List<SetlistEntry> entries(String letter, int count) {
        List<SetlistEntry> list = new ArrayList<>();
        for (int i = 1; i <= count; i++) list.add(entry("%s%02d".formatted(letter, i)));
        return list;
    }

    private List<SetlistEntry> concat(List<SetlistEntry>... parts) {
        List<SetlistEntry> all = new ArrayList<>();
        for (List<SetlistEntry> part : parts) all.addAll(part);
        return all;
    }

    @Test
    void emptyList_givesNoPages() {
        assertThat(splitter.splitInTwo(List.of())).isEmpty();
    }

    @Test
    void singleSong_givesOnePage() {
        assertThat(splitter.splitInTwo(entries("A", 1))).hasSize(1);
    }

    @Test
    void evenCount_splitsExactlyInHalf() {
        List<List<SetlistEntry>> pages = splitter.splitInTwo(entries("A", 10));

        assertThat(pages).hasSize(2);
        assertThat(pages.get(0)).hasSize(5);
        assertThat(pages.get(1)).hasSize(5);
    }

    @Test
    void oddCount_firstPageGetsTheExtraSong() {
        List<List<SetlistEntry>> pages = splitter.splitInTwo(entries("A", 7));

        assertThat(pages.get(0)).hasSize(4);
        assertThat(pages.get(1)).hasSize(3);
    }

    @Test
    void splitIgnoresSetBoundaries_andKeepsOrder() {
        // Real gig shape: A=17, B=19, C=17 → 53 songs → 27 | 26, with B straddling the turn
        List<SetlistEntry> all = concat(entries("A", 17), entries("B", 19), entries("C", 17));

        List<List<SetlistEntry>> pages = splitter.splitInTwo(all);

        assertThat(pages.get(0)).hasSize(27);
        assertThat(pages.get(1)).hasSize(26);
        assertThat(pages.get(0).get(26).getSet()).isEqualTo("B10");
        assertThat(pages.get(1).get(0).getSet()).isEqualTo("B11");
        assertThat(concat(pages.get(0), pages.get(1))).containsExactlyElementsOf(all);
    }
}
