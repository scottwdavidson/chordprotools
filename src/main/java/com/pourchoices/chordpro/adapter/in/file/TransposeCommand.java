package com.pourchoices.chordpro.adapter.in.file;

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
        transposeUseCase.transpose(inputPath, offset, outputPath);
    }
}
