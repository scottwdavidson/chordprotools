package com.pourchoices.chordpro.adapter.in.file;

import com.pourchoices.chordpro.application.domain.model.Setlist;
import com.pourchoices.chordpro.application.domain.model.SetlistEntry;
import com.pourchoices.chordpro.application.port.in.AssignBackingTrackSlotsUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * CLI command that reassigns RC-500 backing-track slot numbers for every song
 * in the target gig's setlist, then regenerates the setlist CSV.
 *
 * <p>Slot allocation:
 * <ul>
 *   <li>In-set songs (SET prefix A–Y) → slots starting at 5, in set order.</li>
 *   <li>Backup songs (SET prefix Z)    → slots starting at 50, alphabetical by title.</li>
 *   <li>Songs with no backing track are skipped; their BACKING value is unchanged.</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>
 *   chordpro-tools assign-backing-track-slots
 *   chordpro-tools assign-backing-track-slots --gig 2026-06-14-rusty-nail
 *   chordpro-tools assign-backing-track-slots --gig 2026-06-14-rusty-nail --output ./rusty-nail-setlist.csv
 * </pre>
 */
@Component
@Command(
        name = "assign-backing-track-slots",
        description = "Reassigns RC-500 slot numbers for all set-assigned backing tracks, " +
                      "updates the catalog and .cho files, then regenerates the setlist CSV."
)
@Slf4j
public class AssignBackingTrackSlotsCommand implements Runnable {

    private static final String DEFAULT_OUTPUT = "./setlist.csv";

    private final AssignBackingTrackSlotsUseCase useCase;

    public AssignBackingTrackSlotsCommand(AssignBackingTrackSlotsUseCase useCase) {
        this.useCase = useCase;
    }

    @Option(
            names = {"--gig", "-g"},
            description = "Gig slug (e.g. 2026-06-14-rusty-nail). " +
                          "Defaults to the lexicographically latest gig in setlist-assignments.csv."
    )
    private String gig;

    @Option(
            names = {"--output", "-o"},
            description = "Output path for the regenerated setlist CSV (default: ${DEFAULT-VALUE})",
            defaultValue = DEFAULT_OUTPUT
    )
    private String outputPath;

    @Override
    public void run() {
        log.info("Assigning RC-500 backing-track slots for gig: {}", gig);

        Setlist setlist = useCase.assignSlots(gig, outputPath);

        System.out.printf("%nBacking-track slot assignment complete — %d songs for gig '%s' written to %s%n%n",
                setlist.size(), setlist.getGig(), outputPath);

        System.out.printf("%-6s  %-40s  %-25s  %-6s  %s%n", "SET", "TITLE", "ARTIST", "KEY", "SLOT");
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
        if (b == null || b.isBlank() || "99".equals(b)) return "—";
        return b;
    }
}
