package com.pourchoices.chordpro.adapter.in.file;

import com.pourchoices.chordpro.application.domain.model.SemanticDiffReport;
import com.pourchoices.chordpro.application.domain.model.SemanticDiffReport.Finding;
import com.pourchoices.chordpro.application.domain.model.SemanticDiffReport.FindingType;
import com.pourchoices.chordpro.application.domain.model.SongId;
import com.pourchoices.chordpro.application.port.in.ConsistentSongDataUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.List;

/**
 * CLI adapter for {@code consistent-song-data}.
 *
 * <p>Checks that every key-variant of a song is semantically identical to
 * the group's reference variant — same lyrics, same harmonic content, once
 * pure transposition and enharmonic respelling are accounted for. Thin
 * catalog-aware wrapper around {@code verify-sync}'s drift engine.
 * Detection only in this phase; no {@code --fix}.
 *
 * <pre>
 *   ./consistent-song-data ABC:B:BobSeger:HollywoodNights
 * </pre>
 */
@Component
@Command(
        name = "consistent-song-data",
        description = "Checks that key-variants of a song share the same lyrics and harmonic "
                + "content (not just metadata), once transposition/enharmonic differences are "
                + "accounted for. Detection only - no --fix in this phase."
)
@Slf4j
public class ConsistentSongDataCommand implements Runnable {

    @Parameters(index = "0",
            description = "Song ID identifying the group to check, e.g. ABC:B:BobSeger:HollywoodNights")
    private String songIdString;

    private final ConsistentSongDataUseCase useCase;

    public ConsistentSongDataCommand(ConsistentSongDataUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    public void run() {
        SongId songId = SongId.parse(songIdString);
        log.info("consistent-song-data: {}", songId);

        List<SemanticDiffReport> reports = useCase.check(songId);

        System.out.printf("%nconsistent-song-data: %s%n%n", songId.toGroupKey());

        if (reports.isEmpty()) {
            System.out.println("No key-variants to compare - nothing to check. \u2713");
            return;
        }

        int totalIssues = 0;
        for (SemanticDiffReport report : reports) {
            System.out.printf("--- %s  vs  %s ---%n", report.getFileA(), report.getFileB());
            for (Finding finding : report.getFindings()) {
                System.out.printf("[%s] line %d: %s%n",
                        label(finding.getType()), finding.getLineNumber(), finding.getDetail());
            }
            if (report.issueCount() == 0) {
                System.out.println("No drift detected. \u2713");
            }
            System.out.println();
            totalIssues += report.issueCount();
        }

        System.out.printf("consistent-song-data: %d variant(s) checked, %d issue(s) total.%n",
                reports.size(), totalIssues);

        // Scriptable exit code: total findings across every variant.
        if (totalIssues > 0) {
            System.exit(totalIssues);
        }
    }

    private static String label(FindingType type) {
        return type == FindingType.LYRIC_DESYNC ? "LYRIC" : "HARMONIC";
    }
}
