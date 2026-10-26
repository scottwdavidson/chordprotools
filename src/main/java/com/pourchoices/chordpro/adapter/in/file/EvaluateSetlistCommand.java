package com.pourchoices.chordpro.adapter.in.file;

import com.pourchoices.chordpro.application.domain.model.ArcThird;
import com.pourchoices.chordpro.application.domain.model.GuitarLayoutRow;
import com.pourchoices.chordpro.application.domain.model.GuitarSwitch;
import com.pourchoices.chordpro.application.domain.model.SetlistEvaluationReport;
import com.pourchoices.chordpro.application.domain.model.SetlistEvaluationReport.Violation;
import com.pourchoices.chordpro.application.port.in.EvaluateSetlistUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;

/**
 * CLI command that evaluates a gig's setlist against its venue's policy
 * (vocal intensity / energy limits, resolved via {@code gig-venues.csv} \u2192
 * {@code venue-profiles.csv}) and reports its energy-arc shape and
 * sing-along placement.
 *
 * <p>Degrades gracefully when characterization data or a venue link is
 * missing \u2014 that's the expected state for most of the catalog today
 * (before Phase 3's bulk backfill), not an error.
 *
 * <pre>
 *   # latest gig
 *   chordpro-tools evaluate-setlist
 *
 *   # specific gig
 *   chordpro-tools evaluate-setlist --gig 2026-06-05-FF
 *
 *   # full guitar plan: every song + its guitar, switches marked
 *   chordpro-tools evaluate-setlist --guitar-layout
 * </pre>
 */
@Component
@Command(
        name = "evaluate-setlist",
        description = "Evaluates a gig's setlist against its venue's policy and energy arc."
)
@Slf4j
public class EvaluateSetlistCommand implements Runnable {

    private static final int COL_TITLE  = 30;
    private static final int COL_ARTIST = 20;
    private static final String SWITCH_MARK = ">>";

    private final EvaluateSetlistUseCase evaluateSetlistUseCase;

    public EvaluateSetlistCommand(EvaluateSetlistUseCase evaluateSetlistUseCase) {
        this.evaluateSetlistUseCase = evaluateSetlistUseCase;
    }

    @Option(
            names = {"--gig", "-g"},
            description = "Gig slug to evaluate (e.g. 2026-06-14-rusty-nail). "
                        + "Defaults to the lexicographically latest gig in gigs.csv."
    )
    private String gig;

    @Option(
            names = {"--guitar-layout", "-l"},
            description = "Show the guitar for EVERY song (switches marked) instead of just the switch list, "
                        + "so the whole guitar plan fits on one page."
    )
    private boolean guitarLayout;

    @Override
    public void run() {
        log.info("Evaluating setlist for gig={}", gig);

        SetlistEvaluationReport report = evaluateSetlistUseCase.evaluateSetlist(gig);

        System.out.println();
        System.out.println("Setlist evaluation — gig '" + report.getGig() + "'");
        System.out.println("Venue: " + (report.getVenue() != null ? report.getVenue() : "(none linked in gig-venues.csv)"));
        System.out.printf("Songs: %d  (uncharacterized — energy: %d, vocal: %d, sing-along: %d, guitar: %d)%n",
                report.getTotalSongs(),
                report.getUncharacterizedEnergyCount(),
                report.getUncharacterizedVocalIntensityCount(),
                report.getUncharacterizedSingAlongCount(),
                report.getUncharacterizedGuitarCount());
        System.out.println();

        System.out.println("Hard violations: " + report.violationCount());
        for (Violation v : report.getViolations()) {
            System.out.printf("  [%-16s] %-6s %-" + COL_TITLE + "s %-" + COL_ARTIST + "s — %s%n",
                    v.getType(), v.getSet(), truncate(v.getTitle(), COL_TITLE), truncate(v.getArtist(), COL_ARTIST), v.getDetail());
        }
        System.out.println();

        System.out.println("Energy arc:");
        for (ArcThird third : report.getArcThirds()) {
            System.out.printf("  %-8s %2d songs (%2d characterized) — avg %s, median %s%n",
                    third.getLabel(), third.getSongCount(), third.getCharacterizedCount(),
                    fmt(third.getAvgEnergy()), fmt(third.getMedianEnergy()));
        }
        for (String note : report.getArcNotes()) {
            System.out.println("  " + note);
        }
        System.out.println();

        System.out.printf("Sing-along: %d total, %d in the final third — %s%n",
                report.getSingAlongCount(),
                report.getSingAlongInFinalThird(),
                report.isLastSongsHaveSingAlong()
                        ? "last few songs include a sing-along, good closer."
                        : "no sing-along among the last few songs.");
        System.out.println();

        System.out.println("Guitar switches: " + report.guitarSwitchCount());
        if (guitarLayout) {
            printGuitarLayout(report.getGuitarLayout());
        } else {
            printGuitarSwitches(report.getGuitarSwitches());
        }
        System.out.println();
    }

    private void printGuitarSwitches(List<GuitarSwitch> switches) {
        for (GuitarSwitch s : switches) {
            System.out.printf("  [%-6s] %-" + COL_TITLE + "s %-" + COL_ARTIST + "s %s -> %s (%s)%n",
                    s.getSet(), truncate(s.getToTitle(), COL_TITLE), truncate(s.getToArtist(), COL_ARTIST),
                    s.getFromGuitar(), s.getToGuitar(),
                    "after \"" + s.getFromTitle() + "\"");
        }
    }

    /** Every song with its guitar, a blank line between sets, and {@value #SWITCH_MARK} on each switch. */
    private void printGuitarLayout(List<GuitarLayoutRow> rows) {
        System.out.println("  (" + SWITCH_MARK + " = switch from previous song, ? = guitar not characterized)");
        System.out.printf("  %-6s %-2s %-" + COL_TITLE + "s %-" + COL_ARTIST + "s %s%n", "Set", "", "Song", "Artist", "Guitar");
        String previousSetLetter = null;
        for (GuitarLayoutRow row : rows) {
            String setLetter = row.getSet().substring(0, 1);
            if (previousSetLetter != null && !previousSetLetter.equals(setLetter)) {
                System.out.println();
            }
            previousSetLetter = setLetter;
            System.out.printf("  %-6s %-2s %-" + COL_TITLE + "s %-" + COL_ARTIST + "s %s%n",
                    row.getSet(), row.isSwitchFromPrevious() ? SWITCH_MARK : "",
                    truncate(row.getTitle(), COL_TITLE), truncate(row.getArtist(), COL_ARTIST),
                    row.getGuitar() == null ? "?" : row.getGuitar());
        }
    }

    private String fmt(Double d) {
        return d == null ? "n/a" : String.format("%.1f", d);
    }

    private String truncate(String value, int maxLen) {
        if (value == null) return "";
        return value.length() <= maxLen ? value : value.substring(0, maxLen - 1) + "\u2026";
    }
}
