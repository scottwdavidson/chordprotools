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

    /** e.g. songs(3) → A01, A02, A03 */
    private List<SetlistEntry> songs(int count) {
        List<SetlistEntry> list = new ArrayList<>();
        for (int i = 1; i <= count; i++) list.add(entry("A%02d".formatted(i)));
        return list;
    }

    private List<Integer> sheetSizes(int songCount) {
        return splitter.paginate(songs(songCount)).stream().map(List::size).toList();
    }

    @Test
    void emptyList_givesNoSheets() {
        assertThat(splitter.paginate(List.of())).isEmpty();
    }

    @Test
    void upTo32Songs_are16PerSheet() {
        assertThat(sheetSizes(10)).containsExactly(10);
        assertThat(sheetSizes(16)).containsExactly(16);
        assertThat(sheetSizes(17)).containsExactly(16, 1);
        assertThat(sheetSizes(30)).containsExactly(16, 14);
        assertThat(sheetSizes(32)).containsExactly(16, 16);
    }

    @Test
    void moreThan32Songs_areThreeSheets_asEvenAsPossible() {
        assertThat(sheetSizes(33)).containsExactly(11, 11, 11);
        assertThat(sheetSizes(49)).containsExactly(17, 17, 15);
        assertThat(sheetSizes(50)).containsExactly(17, 17, 16);
        assertThat(sheetSizes(51)).containsExactly(17, 17, 17);
    }

    @Test
    void ignoresSetBoundaries_andKeepsOrder() {
        List<SetlistEntry> all = new ArrayList<>();
        for (int i = 1; i <= 19; i++) all.add(entry("A%02d".formatted(i)));
        for (int i = 1; i <= 13; i++) all.add(entry("B%02d".formatted(i)));

        List<List<SetlistEntry>> sheets = splitter.paginate(all);

        assertThat(sheets).hasSize(2);
        assertThat(sheets.get(0).get(15).getSet()).isEqualTo("A16");
        assertThat(sheets.get(1).get(0).getSet()).isEqualTo("A17");
        assertThat(sheets.stream().flatMap(List::stream).toList()).containsExactlyElementsOf(all);
    }
}
