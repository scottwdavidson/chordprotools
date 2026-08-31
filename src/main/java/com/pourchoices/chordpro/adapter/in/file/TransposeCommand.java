package com.pourchoices.chordpro.adapter.in.file;

import com.pourchoices.chordpro.application.domain.model.TransposeResult;
import com.pourchoices.chordpro.application.port.in.TransposeUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * CLI adapter for {@code transpose}.
 *
 * <p>Transposes a {@code .cho} file to a new key, rewriting the
 * {@code {key:}} directive and every chord in the body with correct
 * accidental spelling for the target key. {@code --output} is mandatory —
 * this command never overwrites the input file.
 *
 * <p>All presentation lives here, not in {@link com.pourchoices.chordpro.application.domain.service.TransposeService} —
 * the service returns a structured {@link TransposeResult} and this adapter
 * decides what to print.
 *
 * <pre>
 *   ./transpose cho/ABC/B/BobSeger/HollywoodNights.cho --offset 5 --output /tmp/HollywoodNights-b.cho
 * </pre>
 */
@Component
@Command(
        name = "transpose",
        description = "Transposes a ChordPro file to a new key, spelling accidentals "
                + "correctly for the target key. Never overwrites the input file."
)
@Slf4j
public class TransposeCommand implements Runnable {

    @Parameters(index = "0", description = "Path to the input .cho file.")
    private String inputPath;

    @Option(names = "--offset", required = true,
            description = "Semitones to transpose (positive = up, negative = down).")
    private int offset;

    @Option(names = "--output", required = true,
            description = "Path to write the transposed file. Required - never overwrites the input.")
    private String outputPath;

    private final TransposeUseCase transposeUseCase;

    public TransposeCommand(TransposeUseCase transposeUseCase) {
        this.transposeUseCase = transposeUseCase;
    }

    @Override
    public void run() {
        log.info("transpose: {} --offset {} --output {}", inputPath, offset, outputPath);
        TransposeResult result = transposeUseCase.transpose(inputPath, offset, outputPath);
        print(result);
    }

    private void print(TransposeResult result) {
        for (String warning : result.getWarnings()) {
            System.err.printf("WARNING: %s%n", warning);
        }
        int offsetSemitones = result.getOffsetSemitones();
        System.out.printf("Transposed %s: %s -> %s (%+d semitone%s) -> %s%n",
                result.getInputPath(), result.getSourceKeyRaw(), result.targetKeyName(),
                offsetSemitones, Math.abs(offsetSemitones) == 1 ? "" : "s", result.getOutputPath());
    }
}
