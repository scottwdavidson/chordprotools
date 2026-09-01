package com.pourchoices.chordpro.adapter.in.file;

import com.pourchoices.chordpro.application.port.in.CopyGigUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * CLI command that copies all setlist assignments from one gig to a new gig slug,
 * including RC SLOT values.
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
        description = "Copies all setlist assignments (including RC SLOT) from SOURCE-GIG to "
                    + "TARGET-GIG for editing in gigs.csv."
)
@Slf4j
public class CopyGigCommand implements Runnable {

    private final CopyGigUseCase useCase;

    public CopyGigCommand(CopyGigUseCase useCase) {
        this.useCase = useCase;
    }

    @Parameters(index = "0", description = "Gig slug to copy from (must exist in gigs.csv)")
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
            System.out.printf("Edit gigs.csv in Sheets to reorder or swap songs.%n%n");
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
}
