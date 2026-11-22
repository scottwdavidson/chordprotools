package com.pourchoices.chordpro.application.domain.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Unit tests for {@link CatalogStyleChecker}. */
class CatalogStyleCheckerTest {

    private final CatalogStyleChecker checker = new CatalogStyleChecker();

    // ---- titles ----

    @Test
    void initialCapsTitle_hasNoWarnings() {
        assertThat(checker.titleWarnings("Year Of The Cat")).isEmpty();
        assertThat(checker.titleWarnings("Money (That's What I Want)")).isEmpty();
        assertThat(checker.titleWarnings("Dancin', Shaggin' On The Boulevard")).isEmpty();
    }

    @Test
    void aLowercaseSmallWord_isFlagged() {
        assertThat(checker.titleWarnings("Year of the Cat")).singleElement().asString().contains("'of'");
    }

    @Test
    void aLowercaseWordInsideParentheses_isFlagged() {
        assertThat(checker.titleWarnings("Hesitate (partial)")).singleElement().asString().contains("'partial)'");
    }

    @Test
    void aLowercaseWordAfterAHyphenOrSlash_isFlagged() {
        assertThat(checker.titleWarnings("Brown-eyed Girl")).hasSize(1);
        assertThat(checker.titleWarnings("Golden Slumbers/carry That Weight")).hasSize(1);
    }

    @Test
    void lettersInTheMiddleOfAWord_areLeftAlone() {
        assertThat(checker.titleWarnings("McCartney's Song")).isEmpty();
        assertThat(checker.titleWarnings("Don't Stop")).isEmpty();
    }

    @Test
    void curlyQuotes_areFlagged() {
        assertThat(checker.titleWarnings("I\u2019m Gonna Find Another You")).singleElement().asString().contains("curly");
    }

    @Test
    void aNullTitle_isNotAProblem() {
        assertThat(checker.titleWarnings(null)).isEmpty();
    }

    // ---- artist credits ----

    @Test
    void aPrimaryArtistOnly_hasNoWarnings() {
        assertThat(checker.artistWarnings("Hall & Oates")).isEmpty();
        assertThat(checker.artistWarnings("Crosby, Stills, Nash and Young")).isEmpty();
        assertThat(checker.artistWarnings("Tom Petty")).isEmpty();
    }

    @Test
    void collaborationCredits_areFlagged() {
        assertThat(checker.artistWarnings("Chaka Khan / Rufus")).hasSize(1);
        assertThat(checker.artistWarnings("ELO ( Jeff Lynne )")).hasSize(1);
        assertThat(checker.artistWarnings("Leon Bridges ( feat Luke Combs )")).hasSize(1);
        assertThat(checker.artistWarnings("Post Malone ( w/ Morgan Wallen)")).hasSize(1);
        assertThat(checker.artistWarnings("Somebody Featuring Somebody Else")).hasSize(1);
    }

    // ---- look-alike artists ----

    @Test
    void sameArtistSpelledTwoWays_isFoundAgainstKnownArtists() {
        assertThat(checker.similarArtist("Cold Play", List.of("Coldplay", "Tom Petty"))).contains("Coldplay");
        assertThat(checker.similarArtist("Seals & Crofts", List.of("Seals and Crofts"))).contains("Seals and Crofts");
        assertThat(checker.similarArtist("Beatles", List.of("The Beatles"))).contains("The Beatles");
        assertThat(checker.similarArtist("J.J. Cale", List.of("JJ Cale"))).contains("JJ Cale");
    }

    @Test
    void aSingleTypo_isFoundWhenTheNameIsLongEnough() {
        assertThat(checker.similarArtist("Jackson Brown", List.of("Jackson Browne"))).contains("Jackson Browne");
        assertThat(checker.similarArtist("Suzy Quatro", List.of("Suzi Quatro"))).contains("Suzi Quatro");
    }

    @Test
    void theIdenticalSpelling_isNotALookAlike() {
        assertThat(checker.similarArtist("Tom Petty", List.of("Tom Petty"))).isEmpty();
    }

    @Test
    void differentActs_areNotLookAlikes() {
        assertThat(checker.similarArtist("Tom Petty", List.of("Tom Waits", "Tommy Tutone", "Heart"))).isEmpty();
        assertThat(checker.similarArtist("Heart", List.of("Heat"))).isEmpty();      // too short to call a typo
        assertThat(checker.similarArtist("Alan Jackson", List.of("Jackson Browne"))).isEmpty();
    }

    @Test
    void alikePairs_listEachPairOnce() {
        List<String[]> pairs = checker.alikeArtistPairs(
                List.of("Coldplay", "Cold Play", "Cold Play", "Tom Petty", "Seals and Crofts", "Seals & Crofts"));

        assertThat(pairs).hasSize(2);
        assertThat(pairs).anySatisfy(p -> assertThat(p).containsExactly("Cold Play", "Coldplay"));
        assertThat(pairs).anySatisfy(p -> assertThat(p).containsExactly("Seals & Crofts", "Seals and Crofts"));
    }
}
