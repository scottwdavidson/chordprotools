package com.pourchoices.chordpro.adapter.in.file;

import com.pourchoices.chordpro.application.domain.model.TidySongPartsReport;
import com.pourchoices.chordpro.application.domain.model.TidySongPartsReport.Finding;
import com.pourchoices.chordpro.application.domain.model.TidySongPartsReport.FindingType;
import com.pourchoices.chordpro.application.domain.model.TidySongPartsReport.MatchConfidence;
import com.pourchoices.chordpro.application.port.in.TidySongPartsUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;
import java.util.stream.Collectors;

/**
 * CLI adapter for {@code tidy-song-parts}.
 *
 * <p>Rewrites two ad-hoc section-marker shapes across the whole catalog into
 * proper {@code {start_of_part}}/{@code {end_of_part}} directive pairs - see
 * {@link TidySongPartsUseCase}.
 *
 * <pre>
 *   ./tidy-song-parts            # report only (dry-run)
 *   ./tidy-song-parts --fix      # rewrite files
 * </pre>
 */
@Component
@Command(
        name = "tidy-song-parts",
        description = "Rewrites {start_of_bridge}/{end_of_bridge} and {comment: Intro/Bridge/Solo/"
                + "Instrumental/Outro/Pre-Chorus} section markers as {start_of_part}/{end_of_part} pairs."
)
@Slf4j
public class TidySongPartsCommand implements Runnable {

    private final TidySongPartsUseCase useCase;

    public TidySongPartsCommand(TidySongPartsUseCase useCase) {
        this.useCase = useCase;
    }

    @Option(names = "--fix", description = "Rewrite every affected .cho file. Dry-run (report only) by default.")
    private boolean fix;

    @Override
    public void run() {
        TidySongPartsReport report = useCase.run(fix);

        System.out.printf("%ntidy-song-parts%s%n%n", fix ? " (--fix)" : " (dry-run)");

        printFindings("start_of_bridge → start_of_part", report, FindingType.BRIDGE_DIRECTIVE, null);
        printFindings("{comment: ...} header → start_of_part (leading match)", report,
                FindingType.COMMENT_HEADER, MatchConfidence.LEADING);
        printFindings("{comment: ...} header → start_of_part (embedded match - double-check these)", report,
                FindingType.COMMENT_HEADER, MatchConfidence.EMBEDDED);

        long bridgeCount = report.countByType(FindingType.BRIDGE_DIRECTIVE);
        long commentCount = report.countByType(FindingType.COMMENT_HEADER);
        long emptyParts = report.countEmptyParts();

        System.out.println();
        System.out.printf("tidy-song-parts: %d file(s) scanned, %d file(s) %s%n",
                report.getFilesScanned(), report.getFilesChanged(), fix ? "fixed" : "would be fixed");
        System.out.printf("  bridge directives rewritten: %d%n", bridgeCount);
        System.out.printf("  comment headers wrapped:     %d%n", commentCount);
        if (emptyParts > 0) {
            System.out.printf("  WARNING: %d part(s) have zero content lines between header and boundary - "
                    + "worth a quick look%n", emptyParts);
        }

        long totalChanges = bridgeCount + commentCount;
        if (totalChanges == 0) {
            System.out.println("\nNo section markers to tidy. \u2713");
        } else if (!fix) {
            System.out.printf("%nRun with --fix to apply. %d change(s) pending.%n", totalChanges);
        }
    }

    private void printFindings(String heading, TidySongPartsReport report, FindingType type,
                                MatchConfidence confidence) {
        List<Finding> matches = report.getFindings().stream()
                .filter(f -> f.getType() == type)
                .filter(f -> confidence == null || f.getConfidence() == confidence)
                .collect(Collectors.toList());

        if (matches.isEmpty()) {
            return;
        }

        System.out.printf("%n%s (%d)%n", heading, matches.size());
        for (Finding finding : matches) {
            if (type == FindingType.BRIDGE_DIRECTIVE) {
                // Pure token substitution - content is preserved verbatim,
                // so there's no meaningful content-line-count to report.
                System.out.printf("  %s line %d: \"%s\"%n",
                        finding.getFilePath(), finding.getLineNumber(), finding.getLabel());
            } else {
                String emptyFlag = finding.getContentLineCount() == 0 ? " [empty part!]" : "";
                System.out.printf("  %s line %d: \"%s\" (%d content line(s))%s%n",
                        finding.getFilePath(), finding.getLineNumber(), finding.getLabel(),
                        finding.getContentLineCount(), emptyFlag);
            }
        }
    }
}
