package com.pourchoices.chordpro.adapter.in.file;

import com.pourchoices.chordpro.application.port.in.VerifyCatalogUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;

/**
 * CLI adapter for the verify-catalog command.
 *
 * <p>Compares every row in {@code song-catalog.csv} against its {@code .cho} file
 * and reports MISSING FILE or DRIFT where they disagree. Exits with code 1 if
 * any issues are found so the result is scriptable.
 *
 * <pre>
 *   ./verify-catalog
 * </pre>
 */
@Component
@Command(
        name = "verify-catalog",
        description = "Checks that every song-catalog.csv entry matches its .cho file. "
                + "Reports MISSING FILE (file not found) and DRIFT (fields differ). "
                + "Exits with code 1 if any issues are found."
)
@Slf4j
public class VerifyCatalogCommand implements Runnable {

    private final VerifyCatalogUseCase verifyCatalogUseCase;

    public VerifyCatalogCommand(VerifyCatalogUseCase verifyCatalogUseCase) {
        this.verifyCatalogUseCase = verifyCatalogUseCase;
    }

    @Override
    public void run() {
        verifyCatalogUseCase.verifyCatalog();
    }
}
