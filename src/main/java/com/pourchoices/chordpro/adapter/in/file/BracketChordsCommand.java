package com.pourchoices.chordpro.adapter.in.file;

import com.pourchoices.chordpro.application.domain.model.BracketChordsReport;
import com.pourchoices.chordpro.application.domain.model.BracketChordsReport.Finding;
import com.pourchoices.chordpro.application.domain.model.BracketChordsReport.FindingType;
import com.pourchoices.chordpro.application.port.in.BracketChordsUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * CLI adapter for {@code bracket-chords}.
 *
 * <p>Scans every instrumental-notation line (any line with a {@code |}
 * measure separator) in the catalog for three issues: bare (unbracketed)
 * chords, repeat shorthand ({@code :||}, {@code xN}), and strum-slash
 * notation. Only bare chords are auto-fixable - see
 * {@link com.pourchoices.chordpro.application.domain.service.ChordProTransposer#bracketBareChords}.
 * Repeat shorthand and strum-slash are report-only; both need a human call.
 *
 * <pre>
 *   ./bracket-chords            # report only (dry-run)
 *   ./bracket-chords --fix      # wrap bare chords in brackets
 * </pre>
 *
 * <p>Exits with the number of remaining issues (bare chords count only when
 * not fixed) so it's scriptable.
 */
@Component
@Command(
        name = "bracket-chords",
        description = "Finds instrumental-notation drift: bare (unbracketed) chords, "
                + "repeat shorthand, and strum-slash notation. --fix wraps bare chords "
                + "in brackets; the other two are always report-only."
)
@Slf4j
public class BracketChordsCommand implements Runnable {

    private final BracketChordsUseCase useCase;

    public BracketChordsCommand(BracketChordsUseCase useCase) {
        this.useCase = useCase;
    }

    @Option(names = "--fix",
            description = "Wrap bare chords in brackets in every affected .cho file. "
                    + "Repeat shorthand and strum-slash findings are never auto-fixed.")
    private boolean fix;

    @Override
    public void run() {
        BracketChordsReport report = useCase.run(fix);

        System.out.printf("%nbracket-chords%s%n%n", fix ? " (--fix)" : " (dry-run)");

        for (Finding finding : report.getFindings()) {
            System.out.printf("[%s] %s body line %d%n", finding.getType(), finding.getFilePath(), finding.getLineNumber());
            System.out.println("  " + finding.getOriginalLine());
            if (finding.getFixedLine() != null && fix) {
                System.out.println("  -> " + finding.getFixedLine());
            }
        }

        long bareChords = report.countByType(FindingType.BARE_CHORD);
        long repeatShorthand = report.countByType(FindingType.REPEAT_SHORTHAND);
        long strumSlash = report.countByType(FindingType.STRUM_SLASH);

        System.out.println();
        System.out.printf("bracket-chords: %d file(s) scanned, %d file(s) %s%n",
                report.getFilesScanned(), report.getFilesWithBareChords(), fix ? "fixed" : "would be fixed");
        System.out.printf("  bare chords:      %d%s%n", bareChords, fix ? " (fixed)" : " (run with --fix to wrap)");
        System.out.printf("  repeat shorthand: %d (never auto-fixed - needs manual expansion)%n", repeatShorthand);
        System.out.printf("  strum-slash:      %d (never auto-fixed - needs manual review)%n", strumSlash);

        // Scriptable exit code: bare chords only count as an issue until fixed.
        long issues = repeatShorthand + strumSlash + (fix ? 0 : bareChords);
        if (issues == 0) {
            System.out.println("\nNo instrumental-notation issues found. \u2713");
        } else {
            System.exit((int) issues);
        }
    }
}
