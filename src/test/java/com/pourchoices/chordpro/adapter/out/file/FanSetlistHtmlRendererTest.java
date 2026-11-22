package com.pourchoices.chordpro.adapter.out.file;

import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import com.pourchoices.chordpro.application.domain.model.SetlistAssignment;
import com.pourchoices.chordpro.application.domain.model.SetlistEntry;
import com.pourchoices.chordpro.application.domain.model.SongId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Unit tests for {@link FanSetlistHtmlRenderer}. */
class FanSetlistHtmlRendererTest {

    private final FanSetlistHtmlRenderer renderer = new FanSetlistHtmlRenderer();

    private SetlistEntry entry(String set, String title, String artist) {
        CatalogEntry song = CatalogEntry.builder()
                .songId(SongId.parse("ABC:B:TestArtist:" + title.replaceAll("\\W", "")))
                .title(title).artist(artist).key("C").duration("3:00").build();
        SetlistAssignment assignment = SetlistAssignment.builder()
                .gig("2026-11-20-MOODS").songId(song.getSongId()).set(set).rcSlot("45").build();
        return SetlistEntry.builder().song(song).assignment(assignment).build();
    }

    @Test
    void showsTheBand_theVenueAndALongDate() {
        String html = renderer.render("2026-11-20-MOODS", "Moods Wine Bar", List.of(List.of(entry("A01", "One", "Them"))));

        assertThat(html).contains("<h1>Pour Choices</h1>");
        assertThat(html).contains("<p class=\"when\">Moods Wine Bar \u00b7 Friday, November 20, 2026</p>");
    }

    @Test
    void withALogo_theImageReplacesTheTextHeading_andTheVenueAndDateStay() {
        String html = renderer.render("2026-11-20-MOODS", "Moods Wine Bar",
                List.of(List.of(entry("A01", "One", "Them"))), "data:image/png;base64,AAAA");

        assertThat(html).contains("<img class=\"logo\" alt=\"Pour Choices\" src=\"data:image/png;base64,AAAA\">");
        assertThat(html).doesNotContain("<h1>");
        assertThat(html).contains("Moods Wine Bar \u00b7 Friday, November 20, 2026");
    }

    @Test
    void withoutALogo_keepsTheTextHeading() {
        String html = renderer.render("g", null, List.of(List.of(entry("A01", "One", "Them"))), null);

        assertThat(html).contains("<h1>Pour Choices</h1>").doesNotContain("<img");
    }

    @Test
    void withoutAVenue_showsJustTheDate() {
        String html = renderer.render("2026-11-20-MOODS", null, List.of(List.of(entry("A01", "One", "Them"))));

        assertThat(html).contains("<p class=\"when\">Friday, November 20, 2026</p>");
    }

    @Test
    void aSlugWithoutALeadingDate_showsJustTheVenue() {
        String html = renderer.render("pitch-deck", "Blue Vase", List.of(List.of(entry("A01", "One", "Them"))));

        assertThat(html).contains("<p class=\"when\">Blue Vase</p>");
    }

    @Test
    void noVenueAndNoDate_omitsTheLineEntirely() {
        String html = renderer.render("pitch-deck", null, List.of(List.of(entry("A01", "One", "Them"))));

        assertThat(html).doesNotContain("class=\"when\"");
    }

    @Test
    void anImpossibleDateInTheSlug_isNotShown() {
        String html = renderer.render("2026-13-45-oops", null, List.of(List.of(entry("A01", "One", "Them"))));

        assertThat(html).doesNotContain("class=\"when\"");
    }

    @Test
    void eachSetGetsANumberedHeading_inOrder() {
        String html = renderer.render("g", null, List.of(
                List.of(entry("A01", "One", "Them")),
                List.of(entry("C01", "Two", "Those"))));

        // Numbered by position, not by letter: a gig using sets A and C shows "Set 1" and "Set 2".
        assertThat(html).contains("<h2>Set 1</h2>", "<h2>Set 2</h2>").doesNotContain("Set 3");
        assertThat(html.indexOf("Set 1")).isLessThan(html.indexOf("Set 2"));
    }

    @Test
    void eachSongShowsItsTitleAndArtist_butNoBandOnlyCodes() {
        String html = renderer.render("g", null, List.of(List.of(entry("A01", "Starting Over", "Chris Stapleton"))));

        assertThat(html).contains(
                "<li><span class=\"song\">Starting Over</span><span class=\"artist\">Chris Stapleton</span></li>");
        assertThat(html).doesNotContain("A01").doesNotContain("45").doesNotContain("<svg");
    }

    @Test
    void escapesHtmlInTitlesAndArtists() {
        String html = renderer.render("g", "Tom & Jerry's <Bar>", List.of(List.of(
                entry("A01", "Rock & Roll <Part 2>", "Guns N' Roses"))));

        assertThat(html).contains("Rock &amp; Roll &lt;Part 2&gt;", "Guns N&#39; Roses", "Tom &amp; Jerry&#39;s &lt;Bar&gt;");
    }

    @Test
    void laysTheSetsOutSideBySide_oneColumnPerSet() {
        String threeSets = renderer.render("g", null, List.of(
                List.of(entry("A01", "One", "Them")),
                List.of(entry("B01", "Two", "Those")),
                List.of(entry("C01", "Three", "These"))));
        String twoSets = renderer.render("g", null, List.of(
                List.of(entry("A01", "One", "Them")),
                List.of(entry("B01", "Two", "Those"))));

        assertThat(threeSets).contains("style=\"--cols: 3\"");
        assertThat(twoSets).contains("style=\"--cols: 2\"");
    }

    @Test
    void anEmptySetlistStillGetsAValidGrid() {
        String html = renderer.render("g", null, List.of());

        assertThat(html).contains("style=\"--cols: 1\"");
    }

    @Test
    void neverSplitsASongRowAcrossPages() {
        String html = renderer.render("g", null, List.of(List.of(entry("A01", "One", "Them"))));

        assertThat(html).contains("break-inside: avoid");
    }
}
