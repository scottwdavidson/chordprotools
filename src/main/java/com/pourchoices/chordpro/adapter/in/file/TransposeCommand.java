package com.pourchoices.chordpro.adapter.in.file;

import com.pourchoices.chordpro.application.domain.model.TransposeResult;
import com.pourchoices.chordpro.application.domain.model.TransposeSongResult;
import com.pourchoices.chordpro.application.port.in.TransposeSongUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * CLI adapter for {@code transpose}.
 *
 * <p>Creates a new key-variant of a song from its SONG ID: resolves the
 * source {@code .cho} file, transposes it, derives the target SONG
 * ID/filename automatically from the naming convention, and (by default)
 * registers it in {@code song-catalog.csv} — one command instead of a
 * manual {@code transpose} + {@code import-song} two-step.
 *
 * <p>All presentation lives here, not in
 * {@link com.pourchoices.chordpro.application.domain.service.TransposeSongService} —
 * the service returns a structured {@link TransposeSongResult} and this
 * adapter decides what to print.
 *
 * <pre>
 *   ./transpose ABC:B:BobSeger:HollywoodNights --offset 5
 *   ./transpose ABC:B:BobSeger:HollywoodNights-b --offset -2 --no-import
 * </pre>
 */
@Component
@Command(
        name = "transpose",
        description = "Creates a new key-variant of a song: transposes its .cho file and "
                + "(by default) registers it in song-catalog.csv."
)
@Slf4j
public class TransposeCommand implements Runnable {

    @Parameters(index = "0",
            description = "SONG ID to transpose from, e.g. ABC:B:BobSeger:HollywoodNights "
                    + "(see ./find-song-id).")
    private String songId;

    @Option(names = "--offset", required = true,
            description = "Semitones to transpose (positive = up, negative = down).")
    private int offset;

    @Option(names = "--no-import",
            description = "Write the transposed .cho file but skip registering it in song-catalog.csv.",
            defaultValue = "false")
    private boolean noImport;

    private final TransposeSongUseCase transposeSongUseCase;

    public TransposeCommand(TransposeSongUseCase transposeSongUseCase) {
        this.transposeSongUseCase = transposeSongUseCase;
    }

    @Override
    public void run() {
        log.info("transpose: {} --offset {} (no-import={})", songId, offset, noImport);
        TransposeSongResult result = transposeSongUseCase.transposeSong(songId, offset, noImport);
        print(result);
    }

    private void print(TransposeSongResult result) {
        TransposeResult tr = result.getTransposeResult();

        for (String warning : tr.getWarnings()) {
            System.err.printf("WARNING: %s%n", warning);
        }

        int offsetSemitones = tr.getOffsetSemitones();
        System.out.printf("Transposed %s: %s -> %s (%+d semitone%s) -> %s%n",
                tr.getInputPath(), tr.getSourceKeyRaw(), tr.targetKeyName(),
                offsetSemitones, Math.abs(offsetSemitones) == 1 ? "" : "s", tr.getOutputPath());

        if (result.isImportSkipped()) {
            System.out.printf("Catalog import skipped (--no-import). Run ./import-song %s to add it.%n",
                    tr.getOutputPath());
        } else if (result.isImportSuccessful()) {
            System.out.printf("Imported as SONG ID: %s%n", result.getTargetSongId());
        } else {
            System.err.printf("ERROR: transpose succeeded, but catalog import failed: %s%n",
                    result.getImportFailureMessage());
            System.err.printf("The file was still created at %s - fix the issue and run:%n  ./import-song %s%n",
                    tr.getOutputPath(), tr.getOutputPath());
            System.exit(1);
        }
    }
}
