package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import com.pourchoices.chordpro.application.domain.model.ParsedSong;
import com.pourchoices.chordpro.application.domain.model.SongId;
import com.pourchoices.chordpro.application.domain.model.TidySongPartsReport;
import com.pourchoices.chordpro.application.domain.model.TidySongPartsReport.Finding;
import com.pourchoices.chordpro.application.domain.model.TidySongPartsReport.FindingType;
import com.pourchoices.chordpro.application.domain.model.TidySongPartsReport.MatchConfidence;
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
 * Unit tests for {@link TidySongPartsService}. Mirrors the fixture pattern
 * in {@link BracketChordsServiceTest} - a real catalog file is used only so
 * {@code Files.exists()} passes; actual content is fully controlled per
 * test via the mocked {@link ChordProPort}.
 */
@ExtendWith(MockitoExtension.class)
class TidySongPartsServiceTest {

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

    private TidySongPartsService service;

    @BeforeEach
    void setUp() {
        when(catalogConfig.getCatalogIndexPath()).thenReturn("./song-catalog.csv");
        service = new TidySongPartsService(catalogPort, chordProPort,
                new SongParser(new SongLineParser()), new SongLineParser(), catalogConfig);

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

    // ── Transformation A: start_of_bridge / end_of_bridge ──────────────────

    @Test
    void bareBridgeDirective_becomesPartWithDefaultLabel() {
        givenBody("{start_of_bridge}", "lyric line", "{end_of_bridge}");

        TidySongPartsReport report = service.run(true);

        assertThat(report.countByType(FindingType.BRIDGE_DIRECTIVE)).isEqualTo(1);
        List<String> written = captureWrittenSong().getLines();
        assertThat(written).containsExactly("{start_of_part: Bridge}", "lyric line", "{end_of_part}");
    }

    @Test
    void labeledBridgeDirective_preservesLabel() {
        givenBody("{start_of_bridge: Pre-Chorus}", "lyric line", "{end_of_bridge}");

        service.run(true);

        assertThat(captureWrittenSong().getLines())
                .containsExactly("{start_of_part: Pre-Chorus}", "lyric line", "{end_of_part}");
    }

    @Test
    void shorthandSobEob_alsoRewritten() {
        givenBody("{sob}", "lyric line", "{eob}");

        service.run(true);

        assertThat(captureWrittenSong().getLines())
                .containsExactly("{start_of_part: Bridge}", "lyric line", "{end_of_part}");
    }

    // ── Transformation B: {comment: ...} header wrapping ───────────────────

    @Test
    void plainIntroComment_isWrappedUntilStartOfVerse() {
        givenBody(
                "{comment: Intro}",
                "| [Em] . . . | [D] . . . |",
                "",
                "{start_of_verse}",
                "lyrics",
                "{end_of_verse}");

        TidySongPartsReport report = service.run(true);

        assertThat(report.countByType(FindingType.COMMENT_HEADER)).isEqualTo(1);
        Finding finding = report.getFindings().get(0);
        assertThat(finding.getConfidence()).isEqualTo(MatchConfidence.LEADING);
        assertThat(finding.getContentLineCount()).isEqualTo(1);

        assertThat(captureWrittenSong().getLines()).containsExactly(
                "{start_of_part: Intro}",
                "| [Em] . . . | [D] . . . |",
                "{end_of_part}",
                "",
                "{start_of_verse}",
                "lyrics",
                "{end_of_verse}");
    }

    @Test
    void soloHeaderEndsAtNextCommentHeader_notJustStructuralDirective() {
        // Mirrors AlanParsonsProject/DontAnswerMe.cho: Solo sits between two
        // choruses with no verse/chorus directive directly after it.
        givenBody(
                "{end_of_chorus}",
                "",
                "{comment: Solo - unverified}",
                "| [C] . . . |",
                "",
                "{start_of_chorus}",
                "lyrics",
                "{end_of_chorus}");

        service.run(true);

        assertThat(captureWrittenSong().getLines()).containsExactly(
                "{end_of_chorus}",
                "",
                "{start_of_part: Solo - unverified}",
                "| [C] . . . |",
                "{end_of_part}",
                "",
                "{start_of_chorus}",
                "lyrics",
                "{end_of_chorus}");
    }

    @Test
    void outroAtEndOfFile_closesPartEvenWithNoFollowingDirective() {
        givenBody("{comment: Outro}", "last line of the song");

        service.run(true);

        assertThat(captureWrittenSong().getLines()).containsExactly(
                "{start_of_part: Outro}",
                "last line of the song",
                "{end_of_part}");
    }

    @Test
    void unrelatedInlineComment_ridesAlongInsideThePart() {
        // Mirrors Alabama/DancinShagginOnTheBoulevard.cho: a "Jeff double
        // vocal" aside inside the Outro must not be treated as a boundary.
        givenBody(
                "{comment: Outro}",
                "lyric one",
                "{comment: Jeff double vocal}",
                "lyric two");

        service.run(true);

        assertThat(captureWrittenSong().getLines()).containsExactly(
                "{start_of_part: Outro}",
                "lyric one",
                "{comment: Jeff double vocal}",
                "lyric two",
                "{end_of_part}");
    }

    @Test
    void decorativeAsteriskComment_isNeverTouched() {
        givenBody("{comment:***********}", "{start_of_verse}", "lyrics", "{end_of_verse}");

        TidySongPartsReport report = service.run(false);

        assertThat(report.getFindings()).isEmpty();
    }

    @Test
    void embeddedKeyword_guitarSolo_isWrappedButFlaggedEmbedded() {
        givenBody("{comment: Guitar Solo}", "| [C] . . . |", "{start_of_chorus}", "lyrics", "{end_of_chorus}");

        TidySongPartsReport report = service.run(true);

        Finding finding = report.getFindings().get(0);
        assertThat(finding.getConfidence()).isEqualTo(MatchConfidence.EMBEDDED);
        assertThat(finding.getLabel()).isEqualTo("Guitar Solo");
        assertThat(captureWrittenSong().getLines()).containsExactly(
                "{start_of_part: Guitar Solo}", "| [C] . . . |", "{end_of_part}",
                "{start_of_chorus}", "lyrics", "{end_of_chorus}");
    }

    @Test
    void secondBridgeAfterRealBridgeBlock_alsoWrapped() {
        // Mirrors ARS/SoIntoYou.cho: a real {start_of_bridge}/{end_of_bridge}
        // pair, THEN a later {comment: Bridge} reprise with no dedicated
        // directive - both must become start_of_part independently.
        givenBody(
                "{start_of_bridge}",
                "real bridge lyric",
                "{end_of_bridge}",
                "",
                "{comment: Bridge}",
                "reprise lyric",
                "",
                "{start_of_verse}",
                "verse lyric",
                "{end_of_verse}");

        TidySongPartsReport report = service.run(true);

        assertThat(report.countByType(FindingType.BRIDGE_DIRECTIVE)).isEqualTo(1);
        assertThat(report.countByType(FindingType.COMMENT_HEADER)).isEqualTo(1);
        assertThat(captureWrittenSong().getLines()).containsExactly(
                "{start_of_part: Bridge}",
                "real bridge lyric",
                "{end_of_part}",
                "",
                "{start_of_part: Bridge}",
                "reprise lyric",
                "{end_of_part}",
                "",
                "{start_of_verse}",
                "verse lyric",
                "{end_of_verse}");
    }

    @Test
    void emptyPart_zeroContentLines_isStillWrappedAndFlagged() {
        givenBody("{comment: Solo}", "", "{comment: Outro}", "last line");

        TidySongPartsReport report = service.run(true);

        assertThat(report.countEmptyParts()).isEqualTo(1);
        assertThat(captureWrittenSong().getLines()).containsExactly(
                "{start_of_part: Solo}",
                "{end_of_part}",
                "",
                "{start_of_part: Outro}",
                "last line",
                "{end_of_part}");
    }

    @Test
    void alreadyConvertedPart_isIdempotent_noDoubleWrap() {
        givenBody("{start_of_part: Intro}", "lyric", "{end_of_part}");

        TidySongPartsReport report = service.run(true);

        assertThat(report.getFindings()).isEmpty();
        verify(chordProPort, never()).write(any(), any());
    }

    // ── dry-run / --fix behavior ────────────────────────────────────────────

    @Test
    void dryRun_neverWritesFiles() {
        givenBody("{comment: Intro}", "lyric", "{start_of_verse}", "v", "{end_of_verse}");

        service.run(false);

        verify(chordProPort, never()).write(any(), any());
    }

    @Test
    void dryRun_stillReportsWhatWouldChange() {
        givenBody("{comment: Intro}", "lyric", "{start_of_verse}", "v", "{end_of_verse}");

        TidySongPartsReport report = service.run(false);

        assertThat(report.countByType(FindingType.COMMENT_HEADER)).isEqualTo(1);
        assertThat(report.getFilesChanged()).isEqualTo(1);
        verify(chordProPort, never()).write(any(), any());
    }

    @Test
    void cleanSong_reportsNoFindings() {
        givenBody("{start_of_verse}", "lyrics", "{end_of_verse}");

        TidySongPartsReport report = service.run(false);

        assertThat(report.getFindings()).isEmpty();
        assertThat(report.getFilesChanged()).isZero();
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private ParsedSong captureWrittenSong() {
        ArgumentCaptor<ParsedSong> captor = ArgumentCaptor.forClass(ParsedSong.class);
        verify(chordProPort).write(eq(FILE_PATH), captor.capture());
        return captor.getValue();
    }
}
