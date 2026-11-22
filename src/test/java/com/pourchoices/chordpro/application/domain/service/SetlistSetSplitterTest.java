package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import com.pourchoices.chordpro.application.domain.model.SetlistAssignment;
import com.pourchoices.chordpro.application.domain.model.SetlistEntry;
import com.pourchoices.chordpro.application.domain.model.SongId;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Unit tests for {@link SetlistSetSplitter} — pure logic, synthetic entries. */
class SetlistSetSplitterTest {

    private final SetlistSetSplitter splitter = new SetlistSetSplitter();
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

    /** e.g. set('B', 3) → B01, B02, B03 */
    private List<SetlistEntry> set(char letter, int count) {
        List<SetlistEntry> list = new ArrayList<>();
        for (int i = 1; i <= count; i++) list.add(entry("%c%02d".formatted(letter, i)));
        return list;
    }

    private List<SetlistEntry> concat(List<SetlistEntry> first, List<SetlistEntry>... rest) {
        List<SetlistEntry> all = new ArrayList<>(first);
        for (List<SetlistEntry> more : rest) all.addAll(more);
        return all;
    }

    private List<Integer> groupSizes(List<SetlistEntry> entries) {
        return splitter.splitBySet(entries).stream().map(List::size).toList();
    }

    @Test
    void emptyList_givesNoGroups() {
        assertThat(splitter.splitBySet(List.of())).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void threeSets_areThreeGroups() {
        assertThat(groupSizes(concat(set('A', 10), set('B', 9), set('C', 11)))).containsExactly(10, 9, 11);
    }

    @Test
    @SuppressWarnings("unchecked")
    void twoSets_areTwoGroups() {
        assertThat(groupSizes(concat(set('A', 12), set('B', 12)))).containsExactly(12, 12);
    }

    @Test
    void aSingleSet_isOneGroup_evenWhenItIsVeryLong() {
        assertThat(groupSizes(set('A', 40))).containsExactly(40);
    }

    @Test
    @SuppressWarnings("unchecked")
    void aSetIsNeverSplitOrMerged_andOrderIsKept() {
        List<SetlistEntry> all = concat(set('A', 19), set('B', 3), set('D', 2));

        List<List<SetlistEntry>> groups = splitter.splitBySet(all);

        assertThat(groups).hasSize(3);
        assertThat(groups.get(0)).allMatch(e -> e.getSet().startsWith("A"));
        assertThat(groups.get(1)).allMatch(e -> e.getSet().startsWith("B"));
        assertThat(groups.get(2)).allMatch(e -> e.getSet().startsWith("D"));
        assertThat(groups.stream().flatMap(List::stream).toList()).containsExactlyElementsOf(all);
    }
}
