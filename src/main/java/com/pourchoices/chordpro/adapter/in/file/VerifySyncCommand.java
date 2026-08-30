package com.pourchoices.chordpro.adapter.in.file;

import com.pourchoices.chordpro.application.domain.model.SemanticDiffReport;
import com.pourchoices.chordpro.application.domain.model.SemanticDiffReport.Finding;
import com.pourchoices.chordpro.application.domain.model.SemanticDiffReport.FindingType;
import com.pourchoices.chordpro.application.port.in.VerifySyncUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

/**
 * CLI adapter for {@code verify-sync}.
 *
 * <p>Catalog-agnostic drift check between two {@code .cho} files — no
 * {@code SongId} knowledge required. Reports lyric desynchronization and
 * harmonic (chord scale-degree) drift; exits with the number of findings
 * (0 = clean) so it's scriptable.
 *
 * <pre>
 *   ./verify-sync cho/ABC/B/BobSeger/HollywoodNights.cho cho/ABC/B/BobSeger/HollywoodNights-b.cho
 * </pre>
 */
@Component
@Command(
        name = "verify-sync",
        description = "Compares two ChordPro files for semantic drift (lyric or harmonic) "
                + "that survives pure transposition and enharmonic respelling."
)
@Slf4j
public class VerifySyncCommand implements Runnable {

    @Parameters(index = "0", description = "Path to the first .cho file.")
    private String fileA;

    @Parameters(index = "1", description = "Path to the second .cho file.")
    private String fileB;

    private final VerifySyncUseCase verifySyncUseCase;

    public VerifySyncCommand(VerifySyncUseCase verifySyncUseCase) {
        this.verifySyncUseCase = verifySyncUseCase;
    }

    @Override
    public void run() {
        log.info("verify-sync: {} vs {}", fileA, fileB);
        SemanticDiffReport report = verifySyncUseCase.verifySync(fileA, fileB);

        System.out.printf("%nverify-sync: %s  vs  %s%n%n", fileA, fileB);

        for (Finding finding : report.getFindings()) {
            System.out.printf("[%s] line %d: %s%n",
                    label(finding.getType()), finding.getLineNumber(), finding.getDetail());
        }

        System.out.println();
        if (report.issueCount() == 0) {
            System.out.println("No drift detected. \u2713");
        } else {
            System.out.printf("verify-sync: %d issue(s) found.%n", report.issueCount());
        }

        // Scriptable exit code: number of findings.
        if (report.issueCount() > 0) {
            System.exit(report.issueCount());
        }
    }

    private static String label(FindingType type) {
        return type == FindingType.LYRIC_DESYNC ? "LYRIC" : "HARMONIC";
    }
}
