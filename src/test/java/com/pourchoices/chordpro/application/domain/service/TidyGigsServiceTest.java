package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.GigsRowRepair;
import com.pourchoices.chordpro.application.domain.model.SetlistAssignment;
import com.pourchoices.chordpro.application.domain.model.SongId;
import com.pourchoices.chordpro.application.domain.model.TidyGigsResult;
import com.pourchoices.chordpro.application.port.out.SetlistAssignmentsPort;
import com.pourchoices.chordpro.config.ChordproGigsPathConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TidyGigsService} - in particular, that it repairs a
 * malformed raw file (missing trailing column) before ever attempting the
 * strict {@link SetlistAssignmentsPort#readAssignments} parse, and refuses
 * to touch the file at all when a row can't be safely repaired.
 */
class TidyGigsServiceTest {

    private static final String GIGS_PATH_STRING = "./gigs.csv";
    private static final Path GIGS_PATH = Paths.get(GIGS_PATH_STRING);
    private static final String HEADER = "GIG,SONG ID,SET,RC SLOT";

    private SetlistAssignmentsPort assignmentsPort;
    private TidyGigsService service;

    @BeforeEach
    void setUp() {
        assignmentsPort = mock(SetlistAssignmentsPort.class);
        ChordproGigsPathConfig config = mock(ChordproGigsPathConfig.class);
        when(config.getGigsPath()).thenReturn(GIGS_PATH_STRING);
        service = new TidyGigsService(assignmentsPort, config);
    }

    private static SetlistAssignment assignment(String gig, String songId, String set) {
        return SetlistAssignment.builder()
                .gig(gig).songId(SongId.parse(songId)).set(set).build();
    }

    @Test
    void fileMissingOrEmpty_isANoOp() {
        when(assignmentsPort.readRawLines(GIGS_PATH)).thenReturn(List.of());

        TidyGigsResult result = service.tidyGigs();

        assertThat(result.isFileMissingOrEmpty()).isTrue();
        assertThat(result.isSuccessful()).isTrue();
        verify(assignmentsPort, never()).writeRawLines(any(), any());
        verify(assignmentsPort, never()).writeAssignments(any(), any());
    }

    @Test
    void wellFormedFile_sortsAndWritesWithoutTouchingRawLines() {
        List<String> rawLines = List.of(
                HEADER,
                "gig2,ABC:B:BillyJoel:PianoMan,A01,",
                "gig1,ABC:A:AlStewart:YearOfTheCat,A02,"
        );
        when(assignmentsPort.readRawLines(GIGS_PATH)).thenReturn(rawLines);
        when(assignmentsPort.readAssignments(GIGS_PATH)).thenReturn(List.of(
                assignment("gig2", "ABC:B:BillyJoel:PianoMan", "A01"),
                assignment("gig1", "ABC:A:AlStewart:YearOfTheCat", "A02")
        ));

        TidyGigsResult result = service.tidyGigs();

        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.isTidied()).isTrue();
        assertThat(result.hasRepairs()).isFalse();
        verify(assignmentsPort, never()).writeRawLines(any(), any()); // nothing needed repair
        verify(assignmentsPort).writeAssignments(eq(GIGS_PATH), any());
    }

    @Test
    void rowMissingTrailingColumn_isRepairedThenTidiedNormally() {
        List<String> rawLines = List.of(
                HEADER,
                "gig1,ABC:B:BillyJoel:PianoMan,A01" // missing trailing comma
        );
        List<String> repairedLines = List.of(
                HEADER,
                "gig1,ABC:B:BillyJoel:PianoMan,A01,"
        );
        when(assignmentsPort.readRawLines(GIGS_PATH)).thenReturn(rawLines);
        when(assignmentsPort.readAssignments(GIGS_PATH)).thenReturn(List.of(
                assignment("gig1", "ABC:B:BillyJoel:PianoMan", "A01")
        ));

        TidyGigsResult result = service.tidyGigs();

        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.hasRepairs()).isTrue();
        assertThat(result.getRepairedLineNumbers()).containsExactly(2);
        verify(assignmentsPort).writeRawLines(GIGS_PATH, repairedLines);
        verify(assignmentsPort).writeAssignments(any(), any());
    }

    @Test
    void rowWithExtraField_refusesToTouchFileAtAll() {
        List<String> rawLines = List.of(
                HEADER,
                "gig1,ABC:B:BillyJoel:PianoMan,A01,,extra"
        );
        when(assignmentsPort.readRawLines(GIGS_PATH)).thenReturn(rawLines);

        TidyGigsResult result = service.tidyGigs();

        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.getRejectedRows()).hasSize(1);
        assertThat(result.getRejectedRows().get(0))
                .isEqualTo(new GigsRowRepair.RejectedRow(2, "gig1,ABC:B:BillyJoel:PianoMan,A01,,extra"));
        verify(assignmentsPort, never()).writeRawLines(any(), any());
        verify(assignmentsPort, never()).readAssignments(any());
        verify(assignmentsPort, never()).writeAssignments(any(), any());
    }
}
