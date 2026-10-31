package com.pourchoices.chordpro.adapter.out.file;

import com.pourchoices.chordpro.application.domain.model.BackingType;
import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import com.pourchoices.chordpro.application.domain.model.SetlistAssignment;
import com.pourchoices.chordpro.application.domain.model.SetlistEntry;
import com.pourchoices.chordpro.application.domain.model.SongId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Unit tests for {@link SetlistSheetHtmlRenderer}. */
class SetlistSheetHtmlRendererTest {

    private final SetlistSheetHtmlRenderer renderer = new SetlistSheetHtmlRenderer();

    private SetlistEntry entry(String set, String title, BackingType backing, String rcSlot) {
        CatalogEntry song = CatalogEntry.builder()
                .songId(SongId.parse("ABC:B:TestArtist:" + title.replaceAll("\\W", "")))
                .title(title).artist("Test Artist").key("C").duration("3:00")
                .backingType(backing).build();
        SetlistAssignment assignment = SetlistAssignment.builder()
                .gig("2026-01-01-test").songId(song.getSongId()).set(set).rcSlot(rcSlot).build();
        return SetlistEntry.builder().song(song).assignment(assignment).build();
    }

    @Test
    void rendersOneSectionPerPageWithSetTitleAndBackingOnly() {
        String html = renderer.render("2026-01-01-test", List.of(
                List.of(entry("A01", "Starting Over", BackingType.RC, "5")),
                List.of(entry("B01", "Hey California", BackingType.BB, null))));

        assertThat(html).contains("<td class=\"set\">A01</td><td>Starting Over</td><td class=\"backing\">5</td>");
        assertThat(html).contains("<td class=\"set\">B01</td><td>Hey California</td><td class=\"backing\">BB</td>");
        assertThat(html).contains("page 1 of 2").contains("page 2 of 2");
        assertThat(html).doesNotContain("Test Artist");
        assertThat(html.split("<section", -1)).hasSize(3);
    }

    @Test
    void noBackingAndSentinel99_renderBlank() {
        String html = renderer.render("g", List.of(List.of(
                entry("A01", "Plain", null, null),
                entry("A02", "Sentinel", BackingType.RC, "99"))));

        assertThat(html).contains("<td>Plain</td><td class=\"backing\"></td>");
        assertThat(html).contains("<td>Sentinel</td><td class=\"backing\"></td>");
    }

    @Test
    void escapesHtmlInTitles() {
        String html = renderer.render("g", List.of(List.of(
                entry("A01", "Rock & Roll <Part 2>", null, null))));

        assertThat(html).contains("Rock &amp; Roll &lt;Part 2&gt;");
    }

    @Test
    void marksTheFirstSongOfEachNewSetButNotTheFirstRowOfThePage() {
        String html = renderer.render("g", List.of(List.of(
                entry("A01", "One", null, null),
                entry("A02", "Two", null, null),
                entry("B01", "Three", null, null))));

        assertThat(html.split("<tr class=\"new-set\">", -1)).hasSize(2);
        assertThat(html).contains("<tr class=\"new-set\"><td class=\"set\">B01</td>");
    }

    @Test
    void rowHeightShrinksSoTheFullestPageFits() {
        List<SetlistEntry> fullPage = java.util.stream.IntStream.rangeClosed(1, 27)
                .mapToObj(i -> entry("A%02d".formatted(i), "Song " + i, null, null)).toList();

        String html = renderer.render("g", List.of(fullPage));

        // 9.3in / (27 songs + header row) = 0.332in
        assertThat(html).contains("--row-h: 0.332in");
    }
}
