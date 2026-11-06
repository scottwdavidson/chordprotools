package com.pourchoices.chordpro.adapter.out.file;

import com.pourchoices.chordpro.application.domain.model.BackingType;
import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import com.pourchoices.chordpro.application.domain.model.GuitarType;
import com.pourchoices.chordpro.application.domain.model.SetlistAssignment;
import com.pourchoices.chordpro.application.domain.model.SetlistEntry;
import com.pourchoices.chordpro.application.domain.model.SongId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/** Unit tests for {@link SetlistSheetHtmlRenderer}. */
class SetlistSheetHtmlRendererTest {

    private final SetlistSheetHtmlRenderer renderer = new SetlistSheetHtmlRenderer();

    private SetlistEntry entry(String set, String title, BackingType backing, String rcSlot) {
        return entry(set, title, backing, rcSlot, null);
    }

    private SetlistEntry entry(String set, String title, BackingType backing, String rcSlot, GuitarType guitar) {
        CatalogEntry song = CatalogEntry.builder()
                .songId(SongId.parse("ABC:B:TestArtist:" + title.replaceAll("\\W", "")))
                .title(title).artist("Test Artist").key("C").duration("3:00")
                .backingType(backing).guitarType(guitar).build();
        SetlistAssignment assignment = SetlistAssignment.builder()
                .gig("2026-01-01-test").songId(song.getSongId()).set(set).rcSlot(rcSlot).build();
        return SetlistEntry.builder().song(song).assignment(assignment).build();
    }

    private List<SetlistEntry> songs(int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(i -> entry("A%02d".formatted(i), "Song " + i, null, null)).toList();
    }

    @Test
    void rendersCompactSetAndBackingCode_thenTitle() {
        String html = renderer.render("2026-01-01-test", List.of(List.of(
                entry("A01", "Starting Over", BackingType.RC, "45"),
                entry("B09", "Hey California", BackingType.BB, null))));

        assertThat(html).contains("<span class=\"code\">A01-45</span><span class=\"guitar\"></span><span class=\"title\">Starting Over</span>");
        assertThat(html).contains("<span class=\"code\">B09-BB</span><span class=\"guitar\"></span><span class=\"title\">Hey California</span>");
        assertThat(html).doesNotContain("Test Artist");
    }

    @Test
    void noBackingAndSentinel99_showBBAsABeatBuddyReminder() {
        String html = renderer.render("g", List.of(List.of(
                entry("A01", "Plain", null, null),
                entry("A02", "Sentinel", BackingType.RC, "99"),
                entry("A03", "RcWithoutSlot", BackingType.RC, null))));

        assertThat(html).contains("<span class=\"code\">A01-BB</span>");
        assertThat(html).contains("<span class=\"code\">A02-BB</span>");
        assertThat(html).contains("<span class=\"code\">A03-BB</span>");
    }

    @Test
    void guitarIconSitsBetweenTheCodeAndTheTitle() {
        String html = renderer.render("g", List.of(List.of(
                entry("A01", "Strummer", BackingType.RC, "45", GuitarType.ACOUSTIC))));

        assertThat(html).containsPattern("<span class=\"code\">A01-45</span><span class=\"guitar\">"
                + "<svg [^>]*aria-label=\"acoustic guitar\"><use href=\"#guitar-acoustic\"/></svg>"
                + "</span><span class=\"title\">Strummer</span>");
    }

    @Test
    void eachGuitarTypeGetsItsOwnIcon_andUncharacterizedGetsNone() {
        String html = renderer.render("g", List.of(List.of(
                entry("A01", "One", null, null, GuitarType.ACOUSTIC),
                entry("A02", "Two", null, null, GuitarType.ELECTRIC),
                entry("A03", "Three", null, null, GuitarType.BASS),
                entry("A04", "Four", null, null, GuitarType.UNKNOWN),
                entry("A05", "Five", null, null, null))));

        assertThat(html).contains("<use href=\"#guitar-acoustic\"/>", "<use href=\"#guitar-electric\"/>",
                "<use href=\"#guitar-bass\"/>", "<use href=\"#guitar-unknown\"/>");
        assertThat(html).contains("aria-label=\"unknown guitar\"");
        // four icons; the uncharacterized (null) song gets an empty cell
        assertThat(html.split("<svg class=\"guitar-icon\"", -1)).hasSize(5);
        assertThat(html).contains("<span class=\"guitar\"></span><span class=\"title\">Five</span>");
    }

    @Test
    void iconShapesAreDefinedOnceNotPerRow() {
        String html = renderer.render("g", List.of(List.of(
                entry("A01", "One", null, null, GuitarType.ELECTRIC),
                entry("A02", "Two", null, null, GuitarType.ELECTRIC))));

        assertThat(html.split("<symbol id=\"guitar-electric\"", -1)).hasSize(2);
    }

    @Test
    void escapesHtmlInTitles() {
        String html = renderer.render("g", List.of(List.of(
                entry("A01", "Rock & Roll <Part 2>", null, null))));

        assertThat(html).contains("Rock &amp; Roll &lt;Part 2&gt;");
    }

    @Test
    void oneSectionPerSheet_withSheetNumberInTheFooter() {
        String html = renderer.render("2026-01-01-test", List.of(songs(1), songs(1)));

        assertThat(html.split("<section", -1)).hasSize(3);
        assertThat(html).contains("2026-01-01-test &middot; sheet 1 of 2").contains("sheet 2 of 2");
    }

    @Test
    void marksTheFirstSongOfEachNewSet_butNotTheFirstRowOfASheet() {
        String html = renderer.render("g", List.of(List.of(
                entry("A01", "One", null, null),
                entry("A02", "Two", null, null),
                entry("B01", "Three", null, null))));

        assertThat(html.split("<div class=\"row new-set\">", -1)).hasSize(2);
        assertThat(html).contains("<div class=\"row new-set\"><span class=\"code\">B01-BB</span>");
        assertThat(html).contains("<div class=\"row\"><span class=\"code\">A01-BB</span>");
    }

    @Test
    void rowHeightIsSizedForTheStandard16RowSheet() {
        String html = renderer.render("g", List.of(songs(16), songs(3)));

        // (9.8in sheet - 0.3in footer) / 16 rows = 0.594in
        assertThat(html).contains("--row-h: 0.594in");
    }

    @Test
    void aSparseSetlistDoesNotGetTallerRowsThanTheStandardSheet() {
        String html = renderer.render("g", List.of(songs(4)));

        assertThat(html).contains("--row-h: 0.594in");
    }

    @Test
    void aFullerSheetShrinksTheRowsSoItStillFits() {
        String html = renderer.render("g", List.of(songs(17), songs(17), songs(17)));

        // 9.5in / 17 rows = 0.559in
        assertThat(html).contains("--row-h: 0.559in");
    }
}
