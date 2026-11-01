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
    void rendersOneSectionPerSheetWithSetNameFileStemAndBacking() {
        String html = renderer.render("2026-01-01-test", List.of(
                List.of(entry("A01", "Starting Over", BackingType.RC, "5")),
                List.of(entry("B01", "Hey California", BackingType.BB, null))));

        assertThat(html).contains("<span class=\"set\">A01</span>"
                + "<span class=\"title\"><span class=\"name\">Starting Over</span>"
                + "<span class=\"id\">(StartingOver)</span></span>"
                + "<span class=\"backing\">5</span>");
        assertThat(html).contains("<span class=\"name\">Hey California</span>"
                + "<span class=\"id\">(HeyCalifornia)</span></span><span class=\"backing\">BB</span>");
        assertThat(html).contains("Set A &middot; sheet 1 of 2").contains("Set B &middot; sheet 2 of 2");
        assertThat(html).doesNotContain("Test Artist");
        assertThat(html.split("<section", -1)).hasSize(3);
    }

    @Test
    void noBackingAndSentinel99_renderBlank() {
        String html = renderer.render("g", List.of(List.of(
                entry("A01", "Plain", null, null),
                entry("A02", "Sentinel", BackingType.RC, "99"))));

        assertThat(html).contains("(Plain)</span></span><span class=\"backing\"></span>");
        assertThat(html).contains("(Sentinel)</span></span><span class=\"backing\"></span>");
    }

    @Test
    void escapesHtmlInTitles() {
        String html = renderer.render("g", List.of(List.of(
                entry("A01", "Rock & Roll <Part 2>", null, null))));

        assertThat(html).contains("Rock &amp; Roll &lt;Part 2&gt;");
    }

    @Test
    void rowHeightIsSharedAcrossSheets_sizedByTheFullestSheet() {
        List<SetlistEntry> full = java.util.stream.IntStream.rangeClosed(1, 6)
                .mapToObj(i -> entry("A%02d".formatted(i), "Song " + i, null, null)).toList();
        List<SetlistEntry> short1 = List.of(entry("B01", "Short", null, null));

        String html = renderer.render("g", List.of(full, short1));

        // (7.5in sheet - 0.35in footer) / 6 rows = 1.192in
        assertThat(html).contains("--row-h: 1.192in");
    }

    @Test
    void rowHeightIsCapped_soASparseSetlistDoesNotGetAbsurdRows() {
        String html = renderer.render("g", List.of(List.of(entry("A01", "Only", null, null))));

        assertThat(html).contains("--row-h: 1.500in");
    }

    @Test
    void includesTheFitScriptForOverlongTitles() {
        String html = renderer.render("g", List.of(List.of(entry("A01", "x", null, null))));

        assertThat(html).contains("<script>").contains("shrinkToFit");
    }
}
