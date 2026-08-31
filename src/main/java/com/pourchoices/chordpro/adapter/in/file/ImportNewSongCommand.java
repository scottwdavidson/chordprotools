package com.pourchoices.chordpro.adapter.in.file;

import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import com.pourchoices.chordpro.application.domain.model.ImportResult;
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
 * <p>All presentation lives here, not in {@link com.pourchoices.chordpro.application.domain.service.ImportNewSongService} —
 * the service returns a structured {@link ImportResult} and this adapter
 * decides what to print.
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
        ImportResult result = importNewSongUseCase.importNewSong(chordproSongPathString, dryRun);
        print(result);
    }

    private void print(ImportResult result) {
        CatalogEntry entry = result.getCatalogEntry();
        String songIdStr = entry.getSongId().toString();

        if (result.isDryRun()) {
            System.out.println();
            System.out.println("DRY RUN — nothing written to song-catalog.csv");
            System.out.println();
            System.out.printf("  SONG ID   : %s%n", songIdStr);
            System.out.printf("  TITLE     : %s%n", entry.getTitle());
            System.out.printf("  ARTIST    : %s%n", entry.getArtist());
            System.out.printf("  KEY       : %s%n", entry.getKey());
            System.out.printf("  DURATION  : %s%n", entry.getDuration());
            System.out.printf("  TEMPO     : %s%n", nvl(entry.getTempo()));
            System.out.printf("  COUNTIN   : %s%n", nvl(entry.getCountin()));
            System.out.printf("  BACKING   : %s%n",
                    entry.getBackingType() != null ? entry.getBackingType().name() : "");
            System.out.printf("  RC SLOT   : (assigned per gig via assign-backing-track-slots)%n");
            System.out.printf("  NORD      : %s%n", nvl(entry.getNord()));
            System.out.printf("  ROLAND    : %s%n", nvl(entry.getRoland()));
            System.out.printf("  VE        : %s%n", nvl(entry.getVe()));
            System.out.printf("  PERF KEY  : %s%n", nvl(entry.getPerformanceKey()));
            System.out.printf("  SONG LABEL: %s%n", nvl(entry.getSongLabel()));
            System.out.println();
        } else {
            System.out.printf("Imported '%s' (%s) as SONG ID: %s%n",
                    entry.getTitle(), entry.getArtist(), songIdStr);
        }
    }

    private static String nvl(String value) {
        return value != null ? value : "";
    }
}
