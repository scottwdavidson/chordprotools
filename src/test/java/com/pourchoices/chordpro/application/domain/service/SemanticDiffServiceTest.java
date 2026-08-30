package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.SemanticDiffReport;
import com.pourchoices.chordpro.application.domain.model.SemanticDiffReport.Finding;
import com.pourchoices.chordpro.application.domain.model.SemanticDiffReport.FindingType;
import com.pourchoices.chordpro.application.port.out.ChordProPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SemanticDiffService}, per design doc §7.4. Uses real
 * {@link SongParser} / {@link ChordProTransposer} collaborators and mocks
 * only the file-I/O port, matching {@link TransposeServiceTest}'s pattern.
 */
@ExtendWith(MockitoExtension.class)
class SemanticDiffServiceTest {

    @Mock
    ChordProPort chordProPort;

    private SemanticDiffService service;

    @TempDir
    Path tempDir;

    private Path fileA;
    private Path fileB;

    private static final List<String> ORIGINAL = List.of(
            "{title: Test Song}",
            "{key: G}",
            "[G]Hello [D]world [Em]this [C]is a test"
    );

    @BeforeEach
    void setUp() throws IOException {
        service = new SemanticDiffService(chordProPort, new SongParser(new SongLineParser()),
                new ChordProTransposer());
        fileA = tempDir.resolve("A.cho");
        fileB = tempDir.resolve("B.cho");
        Files.createFile(fileA);
        Files.createFile(fileB);
    }

    // ── The whole point: pure transposition must never false-positive ────────

    @Test
    void verifySync_pureTransposition_isClean() {
        when(chordProPort.read(fileA)).thenReturn(ORIGINAL);
        when(chordProPort.read(fileB)).thenReturn(List.of(
                "{title: Test Song}",
                "{key: A}",
                "[A]Hello [E]world [F#m]this [D]is a test"
        ));

        SemanticDiffReport report = service.verifySync(fileA.toString(), fileB.toString());

        assertThat(report.issueCount()).isZero();
    }

    @Test
    void verifySync_enharmonicSpellingOnly_isClean() {
        // A# and Bb are the same chromatic position - not a real drift.
        when(chordProPort.read(fileA)).thenReturn(List.of(
                "{key: C}",
                "[A#]riff"
        ));
        when(chordProPort.read(fileB)).thenReturn(List.of(
                "{key: C}",
                "[Bb]riff"
        ));

        SemanticDiffReport report = service.verifySync(fileA.toString(), fileB.toString());

        assertThat(report.issueCount()).isZero();
    }

    // ── Lyric drift ────────────────────────────────────────────────────────────

    @Test
    void verifySync_genuineLyricChange_reportsLyricDesync() {
        when(chordProPort.read(fileA)).thenReturn(ORIGINAL);
        when(chordProPort.read(fileB)).thenReturn(List.of(
                "{title: Test Song}",
                "{key: G}",
                "[G]Hello [D]world [Em]this [C]is a change"
        ));

        SemanticDiffReport report = service.verifySync(fileA.toString(), fileB.toString());

        assertThat(report.getFindings()).hasSize(1);
        Finding finding = report.getFindings().get(0);
        assertThat(finding.getType()).isEqualTo(FindingType.LYRIC_DESYNC);
        assertThat(finding.getLineNumber()).isEqualTo(1);
    }

    @Test
    void verifySync_lyricDrift_ignoresChordAndDirectiveDifferences() {
        // Same lyrics, different chords/whitespace formatting around them -
        // the lyric check should not care.
        when(chordProPort.read(fileA)).thenReturn(List.of(
                "{key: G}",
                "[G]Hello[D]  world"
        ));
        when(chordProPort.read(fileB)).thenReturn(List.of(
                "{key: G}",
                "[C]Hello [Am]world"
        ));

        SemanticDiffReport report = service.verifySync(fileA.toString(), fileB.toString());

        assertThat(report.getFindings()).noneMatch(f -> f.getType() == FindingType.LYRIC_DESYNC);
    }

    // ── Harmonic drift ───────────────────────────────────────────────────────

    @Test
    void verifySync_genuineChordSubstitution_reportsHarmonicDrift() {
        when(chordProPort.read(fileA)).thenReturn(List.of(
                "{key: C}",
                "[C]Hello [G]world"
        ));
        when(chordProPort.read(fileB)).thenReturn(List.of(
                "{key: C}",
                "[C]Hello [F]world"
        ));

        SemanticDiffReport report = service.verifySync(fileA.toString(), fileB.toString());

        assertThat(report.getFindings()).hasSize(1);
        Finding finding = report.getFindings().get(0);
        assertThat(finding.getType()).isEqualTo(FindingType.HARMONIC_DRIFT);
        assertThat(finding.getLineNumber()).isEqualTo(1);
        assertThat(finding.getDetail()).contains("V").contains("G").contains("IV").contains("F");
    }

    @Test
    void verifySync_extraChordInOneFile_reportsHarmonicDrift() {
        when(chordProPort.read(fileA)).thenReturn(List.of("{key: C}", "[C]Hello"));
        when(chordProPort.read(fileB)).thenReturn(List.of("{key: C}", "[C]Hello[G]"));

        SemanticDiffReport report = service.verifySync(fileA.toString(), fileB.toString());

        assertThat(report.getFindings()).hasSize(1);
        assertThat(report.getFindings().get(0).getType()).isEqualTo(FindingType.HARMONIC_DRIFT);
    }

    // ── Guard-rails ───────────────────────────────────────────────────────────

    @Test
    void verifySync_missingFile_throws() {
        Path missing = tempDir.resolve("DoesNotExist.cho");

        assertThatThrownBy(() -> service.verifySync(missing.toString(), fileB.toString()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File not found");
    }

    @Test
    void verifySync_missingKeyDirective_throws() {
        when(chordProPort.read(fileA)).thenReturn(List.of("{title: No Key}", "[C]Hello"));
        when(chordProPort.read(fileB)).thenReturn(List.of("{key: C}", "[C]Hello"));

        assertThatThrownBy(() -> service.verifySync(fileA.toString(), fileB.toString()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No {key:} directive found");
    }
}
