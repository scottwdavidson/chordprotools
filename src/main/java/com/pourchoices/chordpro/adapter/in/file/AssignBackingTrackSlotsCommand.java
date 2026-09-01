package com.pourchoices.chordpro.adapter.in.file;

import com.pourchoices.chordpro.application.domain.model.AssignBackingTrackSlotsResult;
import com.pourchoices.chordpro.application.domain.model.Setlist;
import com.pourchoices.chordpro.application.domain.model.SetlistEntry;
import com.pourchoices.chordpro.application.port.in.AssignBackingTrackSlotsUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * CLI command that assigns RC-500 backing-track slot numbers for every song
 * in the target gig's setlist, then regenerates the setlist CSV.
 *
 * <p>Default mode <b>preserves</b> whatever's already in {@code gigs.csv} for
 * this gig (typed by hand or set by a previous run) and only fills in blanks -
 * safe to run after every {@code gigs.csv} edit, including a hand edit in a
 * spreadsheet. Pass {@code --reoptimize} to throw away existing values and
 * fully recompute from scratch.
 *
 * <p>Usage:
 * <pre>
 *   chordpro-tools assign-backing-track-slots
 *   chordpro-tools assign-backing-track-slots --gig 2026-06-14-rusty-nail
 *   chordpro-tools assign-backing-track-slots --gig 2026-06-14-rusty-nail --reoptimize
 * </pre>
 */
@Component
@Command(
        name = "assign-backing-track-slots",
        description = "Fills in RC-500 slot numbers for backing tracks with none yet, preserving " +
                      "whatever's already in gigs.csv (hand-typed or previously assigned). " +
                      "Syncs every current slot into its .cho file and regenerates the setlist CSV. " +
                      "Pass --reoptimize to fully recompute every slot from scratch instead."
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
                          "Defaults to the lexicographically latest gig in gigs.csv."
    )
    private String gig;

    @Option(
            names = {"--output", "-o"},
            description = "Output path for the regenerated setlist CSV (default: ${DEFAULT-VALUE})",
            defaultValue = DEFAULT_OUTPUT
    )
    private String outputPath;

    @Option(
            names = {"--reoptimize"},
            description = "Ignore existing RC SLOT values in gigs.csv and fully recompute every " +
                          "slot from scratch, instead of preserving them and only filling blanks.",
            defaultValue = "false"
    )
    private boolean reoptimize;

    @Override
    public void run() {
        log.info("Assigning RC-500 backing-track slots for gig: {} (reoptimize={})", gig, reoptimize);

        try {
            AssignBackingTrackSlotsResult result = useCase.assignSlots(gig, outputPath, reoptimize);
            print(result);
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    private void print(AssignBackingTrackSlotsResult result) {
        Setlist setlist = result.getSetlist();

        if (result.isReoptimized()) {
            System.out.printf(
                    "%nFull re-optimization complete — recomputed all %d RC-500 slot(s) for gig '%s', written to %s%n%n",
                    result.totalSlotted(), setlist.getGig(), outputPath);
        } else {
            System.out.printf(
                    "%nBacking-track slot sync complete for gig '%s' — kept %d existing slot(s), "
                    + "assigned %d new slot(s), written to %s%n%n",
                    setlist.getGig(), result.getPreservedCount(), result.getNewlyAssignedCount(), outputPath);
        }

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
