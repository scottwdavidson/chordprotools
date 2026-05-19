package com.pourchoices.chordpro.adapter.in.file;

import com.pourchoices.chordpro.application.port.in.ImportNewSongUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * CLI adapter for the import-song command.
 *
 * <p>The SONG ID is derived from the file path — the user never constructs it manually.
 *
 * <pre>
 *   ./import-song cho/ABC/B/BillyJoel/MovingOut.cho
 *   ./import-song --dry-run cho/ABC/B/BillyJoel/MovingOut.cho
 * </pre>
 */
@Component
@Command(
        name = "import-song",
        description = "Registers a new .cho file in song-catalog.csv. "
                + "The SONG ID is derived from the file path automatically."
)
@Slf4j
public class ImportNewSongCommand implements Runnable {

    @Parameters(index = "0", description = "Path to the .cho file to import.")
    private String chordproSongPathString;

    @Option(
            names = {"--dry-run", "-n"},
            description = "Preview what would be added without modifying song-catalog.csv.",
            defaultValue = "false"
    )
    private boolean dryRun;

    private final ImportNewSongUseCase importNewSongUseCase;

    public ImportNewSongCommand(ImportNewSongUseCase importNewSongUseCase) {
        this.importNewSongUseCase = importNewSongUseCase;
    }

    @Override
    public void run() {
        log.info("import-song: {} (dry-run={})", chordproSongPathString, dryRun);
        importNewSongUseCase.importNewSong(chordproSongPathString, dryRun);
    }
}
