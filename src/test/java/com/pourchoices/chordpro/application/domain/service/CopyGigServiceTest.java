package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import com.pourchoices.chordpro.application.domain.model.SetlistAssignment;
import com.pourchoices.chordpro.application.domain.model.SongId;
import com.pourchoices.chordpro.application.port.out.CatalogPort;
import com.pourchoices.chordpro.application.port.out.SetlistAssignmentsPort;
import com.pourchoices.chordpro.config.ChordproCatalogIndexPathConfig;
import com.pourchoices.chordpro.config.ChordproSetlistAssignmentsPathConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CopyGigServiceTest {

    @Mock SetlistAssignmentsPort assignmentsPort;
    @Mock CatalogPort catalogPort;
    @Mock ChordproSetlistAssignmentsPathConfig assignmentsConfig;
    @Mock ChordproCatalogIndexPathConfig catalogConfig;

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

    private static CatalogEntry song(String songId, String title, String artist) {
        return CatalogEntry.builder()
                .songId(SongId.parse(songId))
                .title(title)
                .artist(artist)
                .key("C")
                .duration("3:30")
                .build();
    }

    private final List<SetlistAssignment> existingAssignments = List.of(
            assignment(SOURCE_GIG, "ABC:B:BillyJoel:PianoMan", "A01"),
            assignment(SOURCE_GIG, "DEF:E:EltonJohn:Daniel",   "A02"),
            assignment(OTHER_GIG,  "ABC:B:BillyJoel:PianoMan", "B01")
    );

    private final Map<String, CatalogEntry> catalog = Map.of(
            "ABC:B:BillyJoel:PianoMan", song("ABC:B:BillyJoel:PianoMan", "Piano Man", "Billy Joel"),
            "DEF:E:EltonJohn:Daniel",   song("DEF:E:EltonJohn:Daniel",   "Daniel",    "Elton John")
    );

    @BeforeEach
    void setUp() {
        when(assignmentsConfig.getSetlistAssignmentsPath()).thenReturn("./setlist-assignments.csv");
        when(catalogConfig.getCatalogIndexPath()).thenReturn("./song-catalog.csv");
        when(assignmentsPort.readAssignments(any(Path.class))).thenReturn(existingAssignments);
        when(catalogPort.readCatalogFromCsv(any(Path.class))).thenReturn(catalog);
        service = new CopyGigService(assignmentsPort, catalogPort, assignmentsConfig, catalogConfig);
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

        ArgumentCaptor<List<SetlistAssignment>> captor = captureWrittenAssignments();
        List<SetlistAssignment> written = captor.getValue();

        // All original rows preserved, plus 2 new TARG rows = 5 total
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

        ArgumentCaptor<List<SetlistAssignment>> captor = captureWrittenAssignments();
        List<SetlistAssignment> targetRows = captor.getValue().stream()
                .filter(a -> TARGET_GIG.equals(a.getGig()))
                .toList();

        assertThat(targetRows).extracting(SetlistAssignment::getSet)
                .containsExactlyInAnyOrder("A01", "A02");
    }

    @Test
    void copyGig_retainsOtherGigs() {
        service.copyGig(SOURCE_GIG, TARGET_GIG, false);

        ArgumentCaptor<List<SetlistAssignment>> captor = captureWrittenAssignments();
        long otherGigCount = captor.getValue().stream()
                .filter(a -> OTHER_GIG.equals(a.getGig()))
                .count();
        assertThat(otherGigCount).isEqualTo(1);
    }

    @Test
    void copyGig_passesEnrichedCatalogToPort() {
        service.copyGig(SOURCE_GIG, TARGET_GIG, false);

        ArgumentCaptor<Map<String, CatalogEntry>> catalogCaptor = ArgumentCaptor.forClass(Map.class);
        verify(assignmentsPort).writeEnrichedAssignments(
                any(Path.class),
                any(),
                catalogCaptor.capture());

        assertThat(catalogCaptor.getValue()).containsKey("ABC:B:BillyJoel:PianoMan");
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
        // SOURCE_GIG already in existingAssignments; use it as target too
        assertThatThrownBy(() -> service.copyGig(SOURCE_GIG, OTHER_GIG, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already has assignments")
                .hasMessageContaining("--force");
    }

    @Test
    void copyGig_targetAlreadyExists_replaceWithForce() {
        // OTHER_GIG exists; --force should replace its 1 row with SOURCE_GIG's 2 rows
        int count = service.copyGig(SOURCE_GIG, OTHER_GIG, true);
        assertThat(count).isEqualTo(2);

        ArgumentCaptor<List<SetlistAssignment>> captor = captureWrittenAssignments();
        long otherGigCount = captor.getValue().stream()
                .filter(a -> OTHER_GIG.equals(a.getGig()))
                .count();
        assertThat(otherGigCount).isEqualTo(2); // replaced, not added
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<List<SetlistAssignment>> captureWrittenAssignments() {
        ArgumentCaptor<List<SetlistAssignment>> captor = ArgumentCaptor.forClass(List.class);
        verify(assignmentsPort).writeEnrichedAssignments(any(Path.class), captor.capture(), any());
        return captor;
    }
}
