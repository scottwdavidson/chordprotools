package com.pourchoices.chordpro.adapter.in.file;

import com.pourchoices.chordpro.application.port.in.CopyGigUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * CLI command that copies all setlist assignments from one gig to a new gig slug.
 *
 * <p>The copied assignments are written with TITLE and ARTIST columns enriched
 * from the song catalog so the CSV is human-readable in Google Sheets or Excel
 * without cross-referencing {@code song-catalog.csv}.
 *
 * <p>Usage examples:
 * <pre>
 *   # copy last month's gig to the next one
 *   chordpro-tools copy-gig 2026-05-10-rusty-nail 2026-06-14-rusty-nail
 *
 *   # replace an already-started target gig
 *   chordpro-tools copy-gig 2026-05-10-rusty-nail 2026-06-14-rusty-nail --force
 * </pre>
 */
@Component
@Command(
        name = "copy-gig",
        description = "Copies all setlist assignments from SOURCE-GIG to TARGET-GIG. "
                    + "The assignments file is rewritten with TITLE and ARTIST columns "
                    + "populated for easy editing in Google Sheets."
)
@Slf4j
public class CopyGigCommand implements Runnable {

    private final CopyGigUseCase useCase;

    public CopyGigCommand(CopyGigUseCase useCase) {
        this.useCase = useCase;
    }

    @Parameters(index = "0", description = "Gig slug to copy from (must exist in setlist-assignments.csv)")
    private String sourceGig;

    @Parameters(index = "1", description = "New gig slug to create")
    private String targetGig;

    @Option(
            names = {"--force", "-f"},
            description = "Replace existing assignments for TARGET-GIG if it already has any",
            defaultValue = "false"
    )
    private boolean force;

    @Override
    public void run() {
        log.info("Copying gig '{}' → '{}'  (force={})", sourceGig, targetGig, force);
        try {
            int count = useCase.copyGig(sourceGig, targetGig, force);
            System.out.printf("%nCopied %d song(s) from '%s' to '%s'.%n", count, sourceGig, targetGig);
            System.out.printf("Edit setlist-assignments.csv in Sheets to reorder or swap songs.%n%n");
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
}
