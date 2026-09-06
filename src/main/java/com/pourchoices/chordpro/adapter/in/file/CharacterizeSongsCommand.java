package com.pourchoices.chordpro.adapter.in.file;

import com.pourchoices.chordpro.application.domain.model.CharacterizeSongsReport;
import com.pourchoices.chordpro.application.domain.model.CharacterizeSongsReport.FieldTally;
import com.pourchoices.chordpro.application.domain.model.CharacterizeSongsReport.RatingField;
import com.pourchoices.chordpro.application.port.in.CharacterizeSongsUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * CLI adapter for {@code characterize-songs} \u2014 bulk-applies a ratings file
 * onto {@code song-catalog.csv}'s energy/vocal/genre/sing-along fields.
 *
 * <pre>
 *   ./characterize-songs song-ratings.csv            # report only (dry-run)
 *   ./characterize-songs song-ratings.csv --fix      # apply and write the catalog
 * </pre>
 */
@Component
@Command(
        name = "characterize-songs",
        description = "Bulk-applies a ratings file onto song-catalog.csv's energy/vocal/genre/"
                + "sing-along fields. Never overwrites an already-set field. --fix writes the catalog."
)
@Slf4j
public class CharacterizeSongsCommand implements Runnable {

    @Parameters(index = "0", description = "Path to the ratings CSV (see SongRating for the expected columns).")
    private String ratingsInputPath;

    @Option(names = "--fix",
            description = "Write the merged values to song-catalog.csv. Without this, report-only (dry-run).")
    private boolean fix;

    @Option(names = {"--verbose", "-v"},
            description = "List every still-unrated song group by name, not just the count.")
    private boolean verbose;

    private final CharacterizeSongsUseCase useCase;

    public CharacterizeSongsCommand(CharacterizeSongsUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    public void run() {
        log.info("characterize-songs: input={}, fix={}", ratingsInputPath, fix);

        CharacterizeSongsReport report = useCase.characterizeSongs(ratingsInputPath, fix);

        System.out.printf("%ncharacterize-songs%s%n%n", fix ? " (--fix)" : " (dry-run)");
        System.out.printf("Catalog: %d song group(s)%n", report.getTotalCatalogGroups());
        System.out.printf("Ratings: %d row(s) provided, %d matched a catalog group%n",
                report.getRatingRowsProvided(), report.getRatingRowsMatched());
        System.out.println();

        System.out.println("Applied" + (fix ? "" : " (would apply)") + " / already-set-and-skipped, by field:");
        for (RatingField field : RatingField.values()) {
            FieldTally tally = report.getTallies().get(field);
            System.out.printf("  %-16s %4d applied, %4d skipped (already set)%n",
                    field, tally.getApplied(), tally.getAlreadySetSkipped());
        }
        System.out.println();

        if (!report.getUnmatchedRatingGroupKeys().isEmpty()) {
            System.out.println("Unmatched rating rows (no catalog group found \u2014 check for typos):");
            report.getUnmatchedRatingGroupKeys().forEach(k -> System.out.println("  " + k));
            System.out.println();
        }

        System.out.printf("Still unrated (no energy level) after this run: %d of %d group(s)%n",
                report.getStillUnratedGroups().size(), report.getTotalCatalogGroups());
        if (verbose) {
            report.getStillUnratedGroups().forEach(g -> System.out.println("  " + g));
        } else if (!report.getStillUnratedGroups().isEmpty()) {
            System.out.println("(use --verbose to list them all)");
        }
        if (!fix) {
            System.out.println("(dry-run — nothing written to song-catalog.csv; re-run with --fix to apply)");
        }
        System.out.println();
    }
}
