package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.GigSummary;
import com.pourchoices.chordpro.application.domain.model.SetlistAssignment;
import com.pourchoices.chordpro.application.domain.model.SongId;
import com.pourchoices.chordpro.application.port.out.SetlistAssignmentsPort;
import com.pourchoices.chordpro.config.ChordproGigsPathConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ListGigsService — gig counting and chronological sorting.
 * Exercises logic that previously lived (untested) in the list-gigs Python script.
 */
class ListGigsServiceTest {

    private SetlistAssignmentsPort assignmentsPort;
    private ListGigsService service;

    @BeforeEach
    void setUp() {
        assignmentsPort = mock(SetlistAssignmentsPort.class);
        ChordproGigsPathConfig config = mock(ChordproGigsPathConfig.class);
        when(config.getGigsPath()).thenReturn("./gigs.csv");
        service = new ListGigsService(assignmentsPort, config);
    }

    private static SetlistAssignment assignment(String gig, String songId, String set) {
        return SetlistAssignment.builder()
                .gig(gig)
                .songId(SongId.parse(songId))
                .set(set)
                .build();
    }

    private void givenAssignments(SetlistAssignment... assignments) {
        when(assignmentsPort.readAssignments(any())).thenReturn(List.of(assignments));
    }

    // ── Counting ──────────────────────────────────────────────────────────

    @Test
    void countsSongsPerGig() {
        givenAssignments(
                assignment("2026-06-14-rusty-nail", "ABC:B:BillyJoel:MyLife",   "A01"),
                assignment("2026-06-14-rusty-nail", "DEF:E:EltonJohn:Daniel",   "A02"),
                assignment("2026-06-14-rusty-nail", "ABC:B:BillyJoel:Vienna",   "A03"),
                assignment("2025-10-12-theatre",    "DEF:E:EltonJohn:Daniel",   "A01")
        );

        List<GigSummary> result = service.listGigs();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(GigSummary::getGig, GigSummary::getSongCount)
                .containsExactly(
                        // sorted by slug: 2025 before 2026
                        org.assertj.core.groups.Tuple.tuple("2025-10-12-theatre", 1),
                        org.assertj.core.groups.Tuple.tuple("2026-06-14-rusty-nail", 3)
                );
    }

    // ── Sorting ───────────────────────────────────────────────────────────

    @Test
    void gigsSortedBySlug_chronologically() {
        givenAssignments(
                assignment("2026-06-14-rusty-nail", "ABC:B:BillyJoel:MyLife", "A01"),
                assignment("2024-01-01-newyear",    "ABC:B:BillyJoel:MyLife", "A01"),
                assignment("2025-10-12-theatre",    "ABC:B:BillyJoel:MyLife", "A01")
        );

        List<GigSummary> result = service.listGigs();

        assertThat(result).extracting(GigSummary::getGig)
                .containsExactly("2024-01-01-newyear", "2025-10-12-theatre", "2026-06-14-rusty-nail");
    }

    // ── Empty ─────────────────────────────────────────────────────────────

    @Test
    void noAssignments_returnsEmptyList() {
        givenAssignments();
        assertThat(service.listGigs()).isEmpty();
    }

    @Test
    void singleGig_singleSong() {
        givenAssignments(assignment("tbd", "ABC:B:BillyJoel:MyLife", "A01"));

        List<GigSummary> result = service.listGigs();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getGig()).isEqualTo("tbd");
        assertThat(result.get(0).getSongCount()).isEqualTo(1);
    }
}
