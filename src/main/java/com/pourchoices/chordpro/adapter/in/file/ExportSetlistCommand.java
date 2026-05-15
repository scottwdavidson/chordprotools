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
 * <p>Usage examples:
 * <pre>
 *   # latest gig, default output path
 *   chordpro-parser export-setlist
 *
 *   # specific gig
 *   chordpro-parser export-setlist --gig 2026-06-14-rusty-nail
 *
 *   # specific gig, custom output path
 *   chordpro-parser export-setlist --gig 2026-06-14-rusty-nail --output ./rusty-nail-setlist.csv
 * </pre>
 */
@Component
@Command(
        name = "export-setlist",
        description = "Exports a setlist CSV for the target gig, sorted by set order."
)
@Slf4j
public class ExportSetlistCommand implements Runnable {

    private static final String DEFAULT_OUTPUT = "./setlist.csv";

    private final ExportSetlistUseCase exportSetlistUseCase;

    public ExportSetlistCommand(ExportSetlistUseCase exportSetlistUseCase) {
        this.exportSetlistUseCase = exportSetlistUseCase;
    }

    @Option(
            names = {"--gig", "-g"},
            description = "Gig slug to export (e.g. 2026-06-14-rusty-nail). " +
                          "Defaults to the lexicographically latest gig in setlist-assignments.csv."
    )
    private String gig;

    @Option(
            names = {"--output", "-o"},
            description = "Output path for the setlist CSV (default: ${DEFAULT-VALUE})",
            defaultValue = DEFAULT_OUTPUT
    )
    private String outputPath;

    @Override
    public void run() {
        log.info("Exporting setlist (gig={}) to: {}", gig, outputPath);

        Setlist setlist = exportSetlistUseCase.exportSetlist(gig, outputPath);

        System.out.printf("%nSetlist export complete — %d songs for gig '%s' written to %s%n%n",
                setlist.size(), setlist.getGig(), outputPath);

        System.out.printf("%-6s  %-40s  %-25s  %-6s  %s%n", "SET", "TITLE", "ARTIST", "KEY", "BACKING");
        System.out.println("-".repeat(95));
        for (SetlistEntry entry : setlist.getEntries()) {
            System.out.printf("%-6s  %-40s  %-25s  %-6s  %s%n",
                    entry.getSet(),
                    truncate(entry.getTitle(), 40),
                    truncate(entry.getArtist(), 25),
                    resolveKey(entry),
                    resolvedBacking(entry));
        }
        System.out.println();
    }

    private String truncate(String value, int maxLen) {
        if (value == null) return "";
        return value.length() <= maxLen ? value : value.substring(0, maxLen - 1) + "\u2026";
    }

    private String resolveKey(SetlistEntry entry) {
        String pk = entry.getPerformanceKey();
        return (pk != null && !pk.isBlank()) ? pk : entry.getKey();
    }

    private String resolvedBacking(SetlistEntry entry) {
        String b = entry.getBacking();
        if (b == null || b.isBlank() || "99".equals(b)) return "";
        return b;
    }
}
