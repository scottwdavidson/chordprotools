package com.pourchoices.chordpro.adapter.in.file;

import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import com.pourchoices.chordpro.application.domain.model.Setlist;
import com.pourchoices.chordpro.application.port.in.ExportSetlistUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * CLI command that reads the configured song-catalog.csv and exports a new CSV
 * containing only songs that have a Set value assigned, sorted by that value.
 *
 * <p>Usage examples:
 * <pre>
 *   # default output path
 *   chordpro-parser export-setlist
 *
 *   # custom output path
 *   chordpro-parser export-setlist --output ./gig-2025-06-14-setlist.csv
 * </pre>
 */
@Component
@Command(
        name = "export-setlist",
        description = "Exports a setlist CSV containing only songs with a Set value, sorted by set order."
)
@Slf4j
public class ExportSetlistCommand implements Runnable {

    private static final String DEFAULT_OUTPUT = "./setlist.csv";

    private final ExportSetlistUseCase exportSetlistUseCase;

    public ExportSetlistCommand(ExportSetlistUseCase exportSetlistUseCase) {
        this.exportSetlistUseCase = exportSetlistUseCase;
    }

    @Option(
            names = {"--output", "-o"},
            description = "Output path for the setlist CSV (default: ${DEFAULT-VALUE})",
            defaultValue = DEFAULT_OUTPUT
    )
    private String outputPath;

    @Override
    public void run() {
        log.info("Exporting setlist to: {}", outputPath);

        Setlist setlist = exportSetlistUseCase.exportSetlist(outputPath);

        System.out.printf("%nSetlist export complete — %d songs written to %s%n%n", setlist.size(), outputPath);

        // Print a human-readable summary to stdout so Scott can eyeball it right away
        System.out.printf("%-6s  %-40s  %-25s  %-6s  %s%n", "SET", "TITLE", "ARTIST", "KEY", "BACKING");
        System.out.println("-".repeat(95));
        for (CatalogEntry entry : setlist.getEntries()) {
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

    /**
     * Returns the Performance Key when one is set, falling back to the chart key.
     * Mirrors the resolution logic in {@link com.pourchoices.chordpro.adapter.out.file.SetlistAdapter}.
     */
    private String resolveKey(CatalogEntry entry) {
        String pk = entry.getPerformanceKey();
        return (pk != null && !pk.isBlank()) ? pk : entry.getKey();
    }

    /** Blank out the sentinel value so the table stays clean. */
    private String resolvedBacking(CatalogEntry entry) {
        String b = entry.getBacking();
        if (b == null || b.isBlank() || "99".equals(b)) return "";
        return b;
    }
}
