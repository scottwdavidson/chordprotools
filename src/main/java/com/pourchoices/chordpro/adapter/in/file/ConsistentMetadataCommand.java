package com.pourchoices.chordpro.adapter.in.file;

import com.pourchoices.chordpro.application.domain.model.MetadataConsistencyReport;
import com.pourchoices.chordpro.application.domain.model.MetadataConsistencyReport.Finding;
import com.pourchoices.chordpro.application.domain.model.MetadataConsistencyReport.FindingType;
import com.pourchoices.chordpro.application.port.in.ConsistentMetadataUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * CLI adapter for {@code consistent-metadata}.
 *
 * <p>Scans the whole catalog for key-variant metadata inconsistencies. Two
 * checks: cross-variant DRIFT (any field but KEY/CAPO differs) and FILENAME/KEY
 * mismatch. Dry-run by default; {@code --fix} repairs DRIFT in the catalog.
 *
 * <pre>
 *   ./consistent-metadata                       # report only (dry-run)
 *   ./consistent-metadata --fix                 # fix drift from base variants
 *   ./consistent-metadata --fix --source ABC:B:BobSeger:HollywoodNights-b
 * </pre>
 *
 * <p>Exits with the number of issue groups (0 = clean) so it is scriptable.
 */
@Component
@Command(
        name = "consistent-metadata",
        description = "Checks that key-variants of a song share consistent catalog "
                + "metadata (everything but KEY and CAPO, including performance key). "
                + "Also flags filename/key mismatches. Dry-run unless --fix is given."
)
@Slf4j
public class ConsistentMetadataCommand implements Runnable {

    private final ConsistentMetadataUseCase useCase;

    public ConsistentMetadataCommand(ConsistentMetadataUseCase useCase) {
        this.useCase = useCase;
    }

    @Option(names = "--fix",
            description = "Repair cross-variant drift by propagating metadata from the "
                    + "source-of-truth variant into song-catalog.csv. "
                    + "Filename/key mismatches are never auto-fixed.")
    private boolean fix;

    @Option(names = "--source",
            description = "Song ID to treat as the source of truth when fixing. "
                    + "Defaults to each group's base (standard-key) variant.")
    private String sourceSongId;

    @Override
    public void run() {
        MetadataConsistencyReport report = useCase.check(fix, sourceSongId);

        System.out.printf("%nconsistent-metadata%s%n%n", fix ? " (--fix)" : " (dry-run)");

        for (Finding finding : report.getFindings()) {
            System.out.printf("[%s] %s%n", label(finding.getType()), finding.getGroupKey());
            finding.getDetails().forEach(d -> System.out.println("  " + d));
        }

        System.out.println();
        System.out.printf(
                "consistent-metadata: %d group(s) with variants, %d consistent, %d issue(s)%n",
                report.getGroupsChecked(), report.getConsistentGroups(), report.issueCount());

        if (report.issueCount() == 0) {
            System.out.println("All key-variants have consistent metadata. \u2713");
        } else if (!fix) {
            System.out.println("Run with --fix to repair DRIFT "
                    + "(FILENAME/KEY issues must be fixed by hand).");
        }

        // Scriptable exit code: number of issue groups.
        if (report.issueCount() > 0) {
            System.exit(report.issueCount());
        }
    }

    private static String label(FindingType type) {
        return type == FindingType.DRIFT ? "DRIFT" : "FILENAME/KEY";
    }
}
