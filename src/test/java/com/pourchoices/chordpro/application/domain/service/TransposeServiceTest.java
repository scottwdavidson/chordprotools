package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.HeaderDirective;
import com.pourchoices.chordpro.application.domain.model.ParsedSong;
import com.pourchoices.chordpro.application.domain.model.TransposeResult;
import com.pourchoices.chordpro.application.port.out.ChordProPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TransposeService}. Uses the real {@link SongParser}
 * / {@link ChordProTransposer} collaborators (no external dependencies) and
 * mocks only the file-I/O port, per the same pattern as the other service
 * tests in this package.
 */
@ExtendWith(MockitoExtension.class)
class TransposeServiceTest {

    @Mock
    ChordProPort chordProPort;

    private TransposeService service;

    @TempDir
    Path tempDir;

    private Path inputFile;
    private Path outputFile;

    private static final List<String> SIMPLE_SONG = List.of(
            "{title: Test Song}",
            "{artist: Test Artist}",
            "{key: G}",
            "",
            "[G]Hello [D]world [Em]this [C]is a test"
    );

    @BeforeEach
    void setUp() throws IOException {
        service = new TransposeService(chordProPort, new SongParser(new SongLineParser()),
                new ChordProTransposer());
        inputFile = tempDir.resolve("Song.cho");
        Files.createFile(inputFile);
        outputFile = tempDir.resolve("Song-transposed.cho");
    }

    @Test
    void transpose_rewritesKeyDirective() {
        when(chordProPort.read(inputFile)).thenReturn(SIMPLE_SONG);

        service.transpose(inputFile.toString(), 2, outputFile.toString());

        assertThat(keyValue(captureWrittenSong())).isEqualTo("A");
    }

    @Test
    void transpose_returnsStructuredResult() {
        when(chordProPort.read(inputFile)).thenReturn(SIMPLE_SONG);

        TransposeResult result = service.transpose(inputFile.toString(), 2, outputFile.toString());

        assertThat(result.getInputPath()).isEqualTo(inputFile.toString());
        assertThat(result.getOutputPath()).isEqualTo(outputFile.toString());
        assertThat(result.getSourceKeyRaw()).isEqualTo("G");
        assertThat(result.targetKeyName()).isEqualTo("A");
        assertThat(result.getOffsetSemitones()).isEqualTo(2);
        assertThat(result.getWarnings()).isEmpty();
    }

    @Test
    void transpose_collectsWarningsInsteadOfPrinting_forUnrecognizedChordAttempts() {
        when(chordProPort.read(inputFile)).thenReturn(List.of(
                "{title: Test Song}",
                "{artist: Test Artist}",
                "{key: G}",
                "",
                "[Fmjaj7]bad chord attempt"
        ));

        TransposeResult result = service.transpose(inputFile.toString(), 2, outputFile.toString());

        assertThat(result.getWarnings()).hasSize(1);
        assertThat(result.getWarnings().get(0)).contains("Fmjaj7");
    }

    @Test
    void transpose_transposesBodyChords() {
        when(chordProPort.read(inputFile)).thenReturn(SIMPLE_SONG);

        service.transpose(inputFile.toString(), 2, outputFile.toString());

        assertThat(captureWrittenSong().getLines())
                .containsExactly("[A]Hello [E]world [F#m]this [D]is a test");
    }

    @Test
    void transpose_usesFlatSpellingWhenTargetKeyPrefersFlats() {
        // G up 3 semitones -> Bb major, a flat-preference key.
        when(chordProPort.read(inputFile)).thenReturn(SIMPLE_SONG);

        service.transpose(inputFile.toString(), 3, outputFile.toString());

        ParsedSong written = captureWrittenSong();
        assertThat(keyValue(written)).isEqualTo("Bb");
        assertThat(written.getLines()).containsExactly("[Bb]Hello [F]world [Gm]this [Eb]is a test");
    }

    @Test
    void transpose_preservesNonKeyHeaderLines() {
        when(chordProPort.read(inputFile)).thenReturn(SIMPLE_SONG);

        service.transpose(inputFile.toString(), 2, outputFile.toString());

        ParsedSong written = captureWrittenSong();
        assertThat(headerValue(written, HeaderDirective.TITLE)).isEqualTo("Test Song");
        assertThat(headerValue(written, HeaderDirective.ARTIST)).isEqualTo("Test Artist");
    }

    @Test
    void transpose_negativeOffsetWrapsAroundCorrectly() {
        when(chordProPort.read(inputFile)).thenReturn(SIMPLE_SONG);

        service.transpose(inputFile.toString(), -2, outputFile.toString());

        assertThat(keyValue(captureWrittenSong())).isEqualTo("F");
    }

    @Test
    void transpose_zeroOffsetKeepsSameKeyAndChords() {
        when(chordProPort.read(inputFile)).thenReturn(SIMPLE_SONG);

        service.transpose(inputFile.toString(), 0, outputFile.toString());

        ParsedSong written = captureWrittenSong();
        assertThat(keyValue(written)).isEqualTo("G");
        assertThat(written.getLines()).containsExactly("[G]Hello [D]world [Em]this [C]is a test");
    }

    // ── Guard-rails ───────────────────────────────────────────────────────────

    @Test
    void transpose_missingInputFile_throws() {
        Path missing = tempDir.resolve("DoesNotExist.cho");

        assertThatThrownBy(() -> service.transpose(missing.toString(), 2, outputFile.toString()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File not found");
    }

    @Test
    void transpose_outputSameAsInput_throws() {
        assertThatThrownBy(() -> service.transpose(inputFile.toString(), 2, inputFile.toString()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("--output must not be the same file as the input");
    }

    @Test
    void transpose_missingKeyDirective_throws() {
        when(chordProPort.read(inputFile)).thenReturn(List.of(
                "{title: No Key Song}",
                "{artist: Test Artist}",
                "",
                "[C]No key here"
        ));

        assertThatThrownBy(() -> service.transpose(inputFile.toString(), 2, outputFile.toString()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No {key:} directive found");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ParsedSong captureWrittenSong() {
        ArgumentCaptor<ParsedSong> captor = ArgumentCaptor.forClass(ParsedSong.class);
        verify(chordProPort).write(org.mockito.ArgumentMatchers.eq(outputFile), captor.capture());
        return captor.getValue();
    }

    private static String keyValue(ParsedSong song) {
        return headerValue(song, HeaderDirective.KEY);
    }

    private static String headerValue(ParsedSong song, HeaderDirective directive) {
        return song.getParsedHeader().getHeaderLines().stream()
                .filter(l -> l.getHeaderDirective() == directive)
                .findFirst()
                .orElseThrow()
                .getValue();
    }
}
