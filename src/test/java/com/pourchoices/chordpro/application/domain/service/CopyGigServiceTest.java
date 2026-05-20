package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.SetlistAssignment;
import com.pourchoices.chordpro.application.domain.model.SongId;
import com.pourchoices.chordpro.application.port.out.SetlistAssignmentsPort;
import com.pourchoices.chordpro.config.ChordproGigsPathConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CopyGigServiceTest {

    @Mock SetlistAssignmentsPort assignmentsPort;
    @Mock ChordproGigsPathConfig gigsConfig;

    private CopyGigService service;

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private static final String SOURCE_GIG = "2026-05-10-rusty-nail";
    private static final String TARGET_GIG = "2026-06-14-rusty-nail";
    private static final String OTHER_GIG  = "2025-10-12-theatre";

    private static SetlistAssignment assignment(String gig, String songId, String set) {
        return SetlistAssignment.builder()
                .gig(gig)
                .songId(SongId.parse(songId))
                .set(set)
                .build();
    }

    private final List<SetlistAssignment> existingAssignments = List.of(
            assignment(SOURCE_GIG, "ABC:B:BillyJoel:PianoMan", "A01"),
            assignment(SOURCE_GIG, "DEF:E:EltonJohn:Daniel",   "A02"),
            assignment(OTHER_GIG,  "ABC:B:BillyJoel:PianoMan", "B01")
    );

    @BeforeEach
    void setUp() {
        when(gigsConfig.getGigsPath()).thenReturn("./gigs.csv");
        when(assignmentsPort.readAssignments(any(Path.class))).thenReturn(existingAssignments);
        service = new CopyGigService(assignmentsPort, gigsConfig);
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void copyGig_copiesCorrectRowCount() {
        int count = service.copyGig(SOURCE_GIG, TARGET_GIG, false);
        assertThat(count).isEqualTo(2);
    }

    @Test
    void copyGig_newRowsHaveTargetGig() {
        service.copyGig(SOURCE_GIG, TARGET_GIG, false);

        List<SetlistAssignment> written = captureWrittenAssignments();

        // All original rows preserved, plus 2 new target rows = 5 total
        assertThat(written).hasSize(5);
        List<SetlistAssignment> targetRows = written.stream()
                .filter(a -> TARGET_GIG.equals(a.getGig()))
                .toList();
        assertThat(targetRows).hasSize(2);
        assertThat(targetRows).extracting(a -> a.getSongId().toString())
                .containsExactlyInAnyOrder(
                        "ABC:B:BillyJoel:PianoMan",
                        "DEF:E:EltonJohn:Daniel");
    }

    @Test
    void copyGig_preservesSetsFromSource() {
        service.copyGig(SOURCE_GIG, TARGET_GIG, false);

        List<SetlistAssignment> targetRows = captureWrittenAssignments().stream()
                .filter(a -> TARGET_GIG.equals(a.getGig()))
                .toList();

        assertThat(targetRows).extracting(SetlistAssignment::getSet)
                .containsExactlyInAnyOrder("A01", "A02");
    }

    @Test
    void copyGig_retainsOtherGigs() {
        service.copyGig(SOURCE_GIG, TARGET_GIG, false);

        long otherGigCount = captureWrittenAssignments().stream()
                .filter(a -> OTHER_GIG.equals(a.getGig()))
                .count();
        assertThat(otherGigCount).isEqualTo(1);
    }

    // ── Guard-rails ───────────────────────────────────────────────────────────

    @Test
    void copyGig_sourceNotFound_throws() {
        assertThatThrownBy(() -> service.copyGig("2099-01-01-nonexistent", TARGET_GIG, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Source gig not found");
    }

    @Test
    void copyGig_targetAlreadyExists_throwsWithoutForce() {
        assertThatThrownBy(() -> service.copyGig(SOURCE_GIG, OTHER_GIG, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already has assignments")
                .hasMessageContaining("--force");
    }

    @Test
    void copyGig_targetAlreadyExists_replaceWithForce() {
        // OTHER_GIG exists with 1 row; --force should replace it with SOURCE_GIG's 2 rows
        int count = service.copyGig(SOURCE_GIG, OTHER_GIG, true);
        assertThat(count).isEqualTo(2);

        long otherGigCount = captureWrittenAssignments().stream()
                .filter(a -> OTHER_GIG.equals(a.getGig()))
                .count();
        assertThat(otherGigCount).isEqualTo(2); // replaced, not added
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private List<SetlistAssignment> captureWrittenAssignments() {
        ArgumentCaptor<List<SetlistAssignment>> captor = ArgumentCaptor.forClass(List.class);
        verify(assignmentsPort).writeAssignments(any(Path.class), captor.capture());
        return captor.getValue();
    }
}
