package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.BracketChordsReport;
import com.pourchoices.chordpro.application.domain.model.BracketChordsReport.Finding;
import com.pourchoices.chordpro.application.domain.model.BracketChordsReport.FindingType;
import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import com.pourchoices.chordpro.application.domain.model.ParsedSong;
import com.pourchoices.chordpro.application.domain.model.SongId;
import com.pourchoices.chordpro.application.port.out.CatalogPort;
import com.pourchoices.chordpro.application.port.out.ChordProPort;
import com.pourchoices.chordpro.config.ChordproCatalogIndexPathConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link BracketChordsService}.
 *
 * <p>Uses a real, existing catalog file as a fixture purely so
 * {@code Files.exists()} passes ({@code ChordProPath} has no test-injectable
 * base path) - actual line content is fully controlled per test via the
 * mocked {@link ChordProPort}, so what's really on disk is irrelevant.
 * Mirrors the pattern in {@link TransposeServiceTest} (real
 * {@link SongParser}/{@link ChordProTransposer}, mocked I/O port).
 */
@ExtendWith(MockitoExtension.class)
class BracketChordsServiceTest {

    private static final String SONG_ID = "ABC:B:BobSeger:HollywoodNights";
    private static final Path FILE_PATH = Paths.get("./cho/ABC/B/BobSeger/HollywoodNights.cho");

    private static final List<String> HEADER = List.of(
            "{title: Hollywood Nights}",
            "{artist: Bob Seger}",
            "{key: E}",
            "");

    @Mock CatalogPort catalogPort;
    @Mock ChordProPort chordProPort;
    @Mock ChordproCatalogIndexPathConfig catalogConfig;

    private BracketChordsService service;

    @BeforeEach
    void setUp() {
        when(catalogConfig.getCatalogIndexPath()).thenReturn("./song-catalog.csv");
        service = new BracketChordsService(catalogPort, chordProPort,
                new SongParser(new SongLineParser()), new ChordProTransposer(), catalogConfig);

        Map<String, CatalogEntry> catalog = new LinkedHashMap<>();
        catalog.put(SONG_ID, CatalogEntry.builder()
                .songId(SongId.parse(SONG_ID))
                .title("Hollywood Nights")
                .artist("Bob Seger")
                .key("E")
                .duration("4:40")
                .tempo("150")
                .countin("8")
                .performanceKey("E")
                .build());
        when(catalogPort.readCatalogFromCsv(any())).thenReturn(catalog);
    }

    private void givenBody(String... bodyLines) {
        List<String> lines = new ArrayList<>(HEADER);
        lines.addAll(List.of(bodyLines));
        when(chordProPort.read(FILE_PATH)).thenReturn(lines);
    }

    // ── detection (dry-run) ──────────────────────────────────────────────

    @Test
    void bareChordInInstrumentalLine_isFound() {
        givenBody("| C   . . .  | C   . . . |");

        BracketChordsReport report = service.run(false);

        assertThat(report.countByType(FindingType.BARE_CHORD)).isEqualTo(1);
        Finding finding = report.getFindings().get(0);
        assertThat(finding.getFilePath()).isEqualTo("./cho/ABC/B/BobSeger/HollywoodNights.cho");
        assertThat(finding.getFixedLine()).isEqualTo("| [C]   . . .  | [C]   . . . |");
    }

    @Test
    void alreadyBracketedInstrumentalLine_isClean() {
        givenBody("| [C] . . . | [G] . . . |");

        BracketChordsReport report = service.run(false);

        assertThat(report.getFindings()).isEmpty();
    }

    @Test
    void ordinaryChordOverLyricLine_isNeverScanned() {
        // No "|" measure separator - out of scope entirely, even though "Do"
        // and "Am" superficially look chord-adjacent.
        givenBody("[Am]Do you [G]believe in life after love");

        BracketChordsReport report = service.run(false);

        assertThat(report.getFindings()).isEmpty();
    }

    @Test
    void repeatShorthand_isFoundButNeverFixed() {
        givenBody("C | G | A | D :|| x 3");

        BracketChordsReport report = service.run(true);

        assertThat(report.countByType(FindingType.REPEAT_SHORTHAND)).isEqualTo(1);
        // The bare chords on the same line still get fixed independently.
        assertThat(report.countByType(FindingType.BARE_CHORD)).isEqualTo(1);
    }

    @Test
    void strumSlash_isFoundAndNeverTouched() {
        givenBody("| / / / / | / / / / |");

        BracketChordsReport report = service.run(true);

        assertThat(report.countByType(FindingType.STRUM_SLASH)).isEqualTo(1);
        assertThat(report.countByType(FindingType.BARE_CHORD)).isZero();
    }

    @Test
    void cleanCatalog_reportsNoFindings() {
        givenBody("[C]Hello [G]world, [Am]no instrumental here");

        BracketChordsReport report = service.run(false);

        assertThat(report.getFindings()).isEmpty();
        assertThat(report.getFilesScanned()).isEqualTo(1);
        assertThat(report.getFilesWithBareChords()).isZero();
    }

    // ── --fix ─────────────────────────────────────────────────────────────

    @Test
    void fix_writesWrappedLinesBack() {
        givenBody("| C . . . | Am7 . . . |");

        service.run(true);

        ParsedSong written = captureWrittenSong();
        assertThat(written.getLines()).containsExactly("| [C] . . . | [Am7] . . . |");
    }

    @Test
    void fix_preservesHeaderUnchanged() {
        givenBody("| C . . . |");

        service.run(true);

        assertThat(captureWrittenSong().getParsedHeader().getHeaderLines()).isNotEmpty();
    }

    @Test
    void fix_incrementsFilesWithBareChordsOnlyWhenSomethingChanged() {
        givenBody("[C]clean lyric line, nothing to fix");

        BracketChordsReport report = service.run(true);

        assertThat(report.getFilesWithBareChords()).isZero();
        verify(chordProPort, never()).write(any(), any());
    }

    @Test
    void dryRun_neverWritesFiles() {
        givenBody("| C . . . |");

        service.run(false);

        verify(chordProPort, never()).write(any(), any());
    }

    @Test
    void dryRun_stillReportsWhatWouldBeFixed() {
        givenBody("| C . . . |");

        BracketChordsReport report = service.run(false);

        assertThat(report.countByType(FindingType.BARE_CHORD)).isEqualTo(1);
        // Counted as "would be fixed" even though nothing was written yet.
        assertThat(report.getFilesWithBareChords()).isEqualTo(1);
        verify(chordProPort, never()).write(any(), any());
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private ParsedSong captureWrittenSong() {
        ArgumentCaptor<ParsedSong> captor = ArgumentCaptor.forClass(ParsedSong.class);
        verify(chordProPort).write(eq(FILE_PATH), captor.capture());
        return captor.getValue();
    }
}
