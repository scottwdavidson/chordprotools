package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import com.pourchoices.chordpro.application.domain.model.SetlistAssignment;
import com.pourchoices.chordpro.application.domain.model.SetlistEntry;
import com.pourchoices.chordpro.application.domain.model.SongId;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @SafeVarargs
    private List<SetlistEntry> concat(List<SetlistEntry>... parts) {
        List<SetlistEntry> all = new ArrayList<>();
        for (List<SetlistEntry> part : parts) all.addAll(part);
        return all;
    }

    private List<Integer> sizes(List<List<SetlistEntry>> pages) {
        return pages.stream().map(List::size).toList();
    }

    @Test
    void emptyList_givesNoPages() {
        assertThat(splitter.paginate(List.of(), 6)).isEmpty();
    }

    @Test
    void setThatFits_isOnePage() {
        assertThat(sizes(splitter.paginate(entries("A", 6), 6))).containsExactly(6);
    }

    @Test
    void longSet_isSplitEvenly_neverLeavingALonelyLastPage() {
        assertThat(sizes(splitter.paginate(entries("A", 7), 6))).containsExactly(4, 3);
        assertThat(sizes(splitter.paginate(entries("A", 13), 6))).containsExactly(5, 4, 4);
        assertThat(sizes(splitter.paginate(entries("A", 17), 6))).containsExactly(6, 6, 5);
    }

    @Test
    void pagesNeverMixSets_andKeepOrder() {
        List<SetlistEntry> all = concat(entries("A", 8), entries("B", 3), entries("C", 13));

        List<List<SetlistEntry>> pages = splitter.paginate(all, 6);

        assertThat(sizes(pages)).containsExactly(4, 4, 3, 5, 4, 4);
        for (List<SetlistEntry> page : pages) {
            assertThat(page).extracting(e -> e.getSet().charAt(0)).containsOnly(page.get(0).getSet().charAt(0));
        }
        assertThat(pages.stream().flatMap(List::stream).toList()).containsExactlyElementsOf(all);
    }

    @Test
    void gapsInSetNumbering_doNotStartANewPage() {
        // A removed song leaves e.g. C08, C10 — still the same set, still the same page.
        List<SetlistEntry> set = List.of(entry("C08"), entry("C10"), entry("C11"));

        assertThat(sizes(splitter.paginate(set, 6))).containsExactly(3);
    }

    @Test
    void rejectsNonPositiveRowCap() {
        assertThatThrownBy(() -> splitter.paginate(entries("A", 3), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
