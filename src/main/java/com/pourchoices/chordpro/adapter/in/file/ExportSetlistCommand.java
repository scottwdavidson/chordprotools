package com.pourchoices.chordpro.adapter.in.file;

import com.pourchoices.chordpro.application.domain.model.Setlist;
import com.pourchoices.chordpro.application.domain.model.SetlistEntry;
import com.pourchoices.chordpro.application.port.in.ExportSetlistUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * CLI command that joins the song catalog with setlist assignments for the target
 * gig and exports a setlist CSV sorted by set order.
 *
 * <p>Default output (fan-facing, printable):
 * Only non-Z songs (A–Y sets) are included in both the CSV and the console table.
 *
 * <p>With {@code --verbose}: the full list including Z-set backup songs is shown,
 * with a visual separator between the fan setlist and the backup section.
 *
 * <pre>
 *   # latest gig, fan setlist only (default)
 *   chordpro-tools export-setlist
 *
 *   # full list including backup songs
 *   chordpro-tools export-setlist --verbose
 *
 *   # specific gig, verbose
 *   chordpro-tools export-setlist --gig 2026-06-14-rusty-nail --verbose
 *
 *   # custom output path
 *   chordpro-tools export-setlist --output ./rusty-nail-setlist.csv
 * </pre>
 */
@Component
@Command(
        name = "export-setlist",
        description = "Exports a setlist CSV for the target gig, sorted by set order. "
                + "By default only fan-facing sets (A\u2013Y) are included. "
                + "Use --verbose to include Z-set backup songs."
)
@Slf4j
public class ExportSetlistCommand implements Runnable {

    private static final String DEFAULT_OUTPUT = "./setlist.csv";
    private static final int    COL_TITLE  = 40;
    private static final int    COL_ARTIST = 25;

    private final ExportSetlistUseCase exportSetlistUseCase;

    public ExportSetlistCommand(ExportSetlistUseCase exportSetlistUseCase) {
        this.exportSetlistUseCase = exportSetlistUseCase;
    }

    @Option(
            names = {"--gig", "-g"},
            description = "Gig slug to export (e.g. 2026-06-14-rusty-nail). "
                        + "Defaults to the lexicographically latest gig in gigs.csv."
    )
    private String gig;

    @Option(
            names = {"--output", "-o"},
            description = "Output path for the setlist CSV (default: ${DEFAULT-VALUE})",
            defaultValue = DEFAULT_OUTPUT
    )
    private String outputPath;

    @Option(
            names = {"--verbose", "-v"},
            description = "Include Z-set backup songs in both the CSV output and console display.",
            defaultValue = "false"
    )
    private boolean verbose;

    @Override
    public void run() {
        log.info("Exporting setlist (gig={}, verbose={}) to: {}", gig, verbose, outputPath);

        Setlist setlist = exportSetlistUseCase.exportSetlist(gig, outputPath, verbose);

        System.out.printf("%nSetlist export complete — %d songs for gig '%s' written to %s%n",
                setlist.size(), setlist.getGig(), outputPath);
        if (!verbose) {
            System.out.println("  (backup / Z-set songs excluded — use --verbose to include them)");
        }
        System.out.println();

        printHeader();
        boolean inBackupSection = false;

        for (SetlistEntry entry : setlist.getEntries()) {
            boolean isBackup = entry.getSet().toUpperCase().startsWith("Z");

            if (isBackup && !inBackupSection) {
                inBackupSection = true;
                System.out.println();
                System.out.println("  BACKUP / Z-SET");
                printHeader();
            }

            System.out.printf("%-6s  %-" + COL_TITLE + "s  %-" + COL_ARTIST + "s  %-6s  %s%n",
                    entry.getSet(),
                    truncate(entry.getTitle(),  COL_TITLE),
                    truncate(entry.getArtist(), COL_ARTIST),
                    resolveKey(entry),
                    entry.getBacking());
        }
        System.out.println();
    }

    private void printHeader() {
        System.out.printf("%-6s  %-" + COL_TITLE + "s  %-" + COL_ARTIST + "s  %-6s  %s%n",
                "SET", "TITLE", "ARTIST", "KEY", "BACKING");
        System.out.println("-".repeat(95));
    }

    private String truncate(String value, int maxLen) {
        if (value == null) return "";
        return value.length() <= maxLen ? value : value.substring(0, maxLen - 1) + "\u2026";
    }

    private String resolveKey(SetlistEntry entry) {
        String pk = entry.getPerformanceKey();
        return (pk != null && !pk.isBlank()) ? pk : entry.getKey();
    }
}
