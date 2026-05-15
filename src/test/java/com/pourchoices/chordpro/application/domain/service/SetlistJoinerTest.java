package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import com.pourchoices.chordpro.application.domain.model.SetlistAssignment;
import com.pourchoices.chordpro.application.domain.model.SetlistEntry;
import com.pourchoices.chordpro.application.domain.model.SongId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for SetlistJoiner — gig resolution and catalog join logic.
 */
class SetlistJoinerTest {

    private final SetlistJoiner joiner = new SetlistJoiner();

    // ── Fixtures ──────────────────────────────────────────────────────────

    private static final String GIG_RUSTY  = "2026-06-14-rusty-nail";
    private static final String GIG_THEATRE = "2025-10-12-theatre";

    private static CatalogEntry song(String songId, String title) {
        return CatalogEntry.builder()
                .songId(SongId.parse(songId))
                .title(title)
                .artist("Test Artist")
                .key("C")
                .duration("3:00")
                .build();
    }

    private static SetlistAssignment assignment(String gig, String songId, String set) {
        return SetlistAssignment.builder()
                .gig(gig)
                .songId(SongId.parse(songId))
                .set(set)
                .build();
    }

    // ── resolveGig ────────────────────────────────────────────────────────

    @Test
    void resolveGig_explicitParam_returnsAsIs() {
        List<SetlistAssignment> assignments = List.of(
                assignment(GIG_THEATRE, "DEF:E:EltonJohn:Daniel", "A01")
        );
        assertThat(joiner.resolveGig(GIG_RUSTY, assignments)).isEqualTo(GIG_RUSTY);
    }

    @Test
    void resolveGig_nullParam_returnsLatestGig() {
        List<SetlistAssignment> assignments = List.of(
                assignment(GIG_THEATRE, "DEF:E:EltonJohn:Daniel", "A01"),
                assignment(GIG_RUSTY,   "DEF:E:EltonJohn:Daniel", "A01")
        );
        // GIG_RUSTY is lexicographically last (2026 > 2025)
        assertThat(joiner.resolveGig(null, assignments)).isEqualTo(GIG_RUSTY);
    }

    @Test
    void resolveGig_blankParam_returnsLatestGig() {
        List<SetlistAssignment> assignments = List.of(
                assignment(GIG_THEATRE, "DEF:E:EltonJohn:Daniel", "A01")
        );
        assertThat(joiner.resolveGig("  ", assignments)).isEqualTo(GIG_THEATRE);
    }

    @Test
    void resolveGig_noAssignments_returnsNull() {
        assertThat(joiner.resolveGig(null, List.of())).isNull();
    }

    // ── join ─────────────────────────────────────────────────────────────

    @Test
    void join_matchingAssignments_returnsSetlistEntries() {
        CatalogEntry daniel = song("DEF:E:EltonJohn:Daniel", "Daniel");
        CatalogEntry csny   = song("ABC:C:CSNY:JustASong", "Just a Song Before I Go");

        Map<String, CatalogEntry> catalog = Map.of(
                "DEF:E:EltonJohn:Daniel", daniel,
                "ABC:C:CSNY:JustASong",  csny
        );

        List<SetlistAssignment> assignments = List.of(
                assignment(GIG_RUSTY, "DEF:E:EltonJohn:Daniel", "A01"),
                assignment(GIG_RUSTY, "ABC:C:CSNY:JustASong",   "A02"),
                assignment(GIG_THEATRE, "DEF:E:EltonJohn:Daniel", "A01") // different gig
        );

        List<SetlistEntry> result = joiner.join(GIG_RUSTY, assignments, catalog);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getSet()).isEqualTo("A01");
        assertThat(result.get(0).getTitle()).isEqualTo("Daniel");
        assertThat(result.get(0).getGig()).isEqualTo(GIG_RUSTY);
        assertThat(result.get(1).getSet()).isEqualTo("A02");
    }

    @Test
    void join_noAssignments_returnsEmpty() {
        Map<String, CatalogEntry> catalog = Map.of(
                "DEF:E:EltonJohn:Daniel", song("DEF:E:EltonJohn:Daniel", "Daniel")
        );
        assertThat(joiner.join(GIG_RUSTY, List.of(), catalog)).isEmpty();
    }

    @Test
    void join_nullGigParam_autoResolvesToLatest() {
        CatalogEntry daniel = song("DEF:E:EltonJohn:Daniel", "Daniel");
        Map<String, CatalogEntry> catalog = Map.of("DEF:E:EltonJohn:Daniel", daniel);

        List<SetlistAssignment> assignments = List.of(
                assignment(GIG_THEATRE, "DEF:E:EltonJohn:Daniel", "A01"),
                assignment(GIG_RUSTY,   "DEF:E:EltonJohn:Daniel", "A02")
        );

        List<SetlistEntry> result = joiner.join(null, assignments, catalog);

        // Should resolve to GIG_RUSTY (lexicographically last) and return its 1 entry
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getGig()).isEqualTo(GIG_RUSTY);
        assertThat(result.get(0).getSet()).isEqualTo("A02");
    }

    @Test
    void join_danglingAssignment_skipsAndReturnsRest() {
        CatalogEntry daniel = song("DEF:E:EltonJohn:Daniel", "Daniel");
        Map<String, CatalogEntry> catalog = Map.of("DEF:E:EltonJohn:Daniel", daniel);

        List<SetlistAssignment> assignments = List.of(
                assignment(GIG_RUSTY, "DEF:E:EltonJohn:Daniel", "A01"),
                assignment(GIG_RUSTY, "XYZ:X:Unknown:GhostSong", "A02") // not in catalog
        );

        List<SetlistEntry> result = joiner.join(GIG_RUSTY, assignments, catalog);

        // Ghost song is skipped; Daniel is returned
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Daniel");
    }

    @Test
    void join_noAssignmentsForRequestedGig_returnsEmpty() {
        CatalogEntry daniel = song("DEF:E:EltonJohn:Daniel", "Daniel");
        Map<String, CatalogEntry> catalog = Map.of("DEF:E:EltonJohn:Daniel", daniel);

        List<SetlistAssignment> assignments = List.of(
                assignment(GIG_THEATRE, "DEF:E:EltonJohn:Daniel", "A01")
        );

        List<SetlistEntry> result = joiner.join(GIG_RUSTY, assignments, catalog);

        assertThat(result).isEmpty();
    }
}
