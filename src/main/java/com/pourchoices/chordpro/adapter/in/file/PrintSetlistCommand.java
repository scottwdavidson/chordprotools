package com.pourchoices.chordpro.adapter.in.file;

import com.pourchoices.chordpro.application.domain.model.Setlist;
import com.pourchoices.chordpro.application.domain.model.SetlistEntry;
import com.pourchoices.chordpro.application.port.in.PrintSetlistUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;

/**
 * CLI command that writes a floor-sheet setlist for the gig: landscape pages, one set per
 * page, as-big-as-possible SET, song name (the 12-char RC-500 SONG LABEL) and BACKING. Open the
 * result in a browser, print it,
 * and tape it down as the "what's the next song" double check.
 *
 * <pre>
 *   # latest gig → ./setlist.html
 *   chordpro-tools print-setlist
 *
 *   # specific gig, custom output
 *   chordpro-tools print-setlist --gig 2026-06-14-rusty-nail --output ./rusty-nail.html
 * </pre>
 */
@Component
@Command(
        name = "print-setlist",
        description = "Writes a huge-type, one-set-per-page landscape floor setlist (set, title, backing) "
                + "as HTML. Backup / Z-set songs are never included."
)
@Slf4j
public class PrintSetlistCommand implements Runnable {

    private static final String DEFAULT_OUTPUT = "./setlist.html";
    private static final int DEFAULT_ROWS_PER_PAGE = 6;

    private final PrintSetlistUseCase printSetlistUseCase;

    public PrintSetlistCommand(PrintSetlistUseCase printSetlistUseCase) {
        this.printSetlistUseCase = printSetlistUseCase;
    }

    @Option(
            names = {"--gig", "-g"},
            description = "Gig slug to print (e.g. 2026-06-14-rusty-nail). "
                        + "Defaults to the lexicographically latest gig in gigs.csv."
    )
    private String gig;

    @Option(
            names = {"--output", "-o"},
            description = "Output path for the printable HTML (default: ${DEFAULT-VALUE})",
            defaultValue = DEFAULT_OUTPUT
    )
    private String outputPath;

    @Option(
            names = {"--rows-per-page", "-r"},
            description = "Most songs on any one page; fewer rows means bigger type (default: ${DEFAULT-VALUE})",
            defaultValue = "" + DEFAULT_ROWS_PER_PAGE
    )
    private int rowsPerPage;

    @Override
    public void run() {
        log.info("Printing setlist (gig={}) to: {}", gig, outputPath);

        Setlist setlist = printSetlistUseCase.printSetlist(gig, outputPath, rowsPerPage);

        System.out.printf("%nPrintable setlist ready — %d songs for gig '%s' written to %s%n",
                setlist.size(), setlist.getGig(), outputPath);
        System.out.println("  Open it in a browser and print: Landscape, Letter, margins Default, headers/footers off.");
        reportSongsWithoutLabel(setlist);
        System.out.println();
    }

    /** Songs with no RC-500 SONG LABEL print their full title instead — say which, so they can be labelled. */
    private void reportSongsWithoutLabel(Setlist setlist) {
        List<SetlistEntry> unlabelled = setlist.getEntries().stream()
                .filter(entry -> !entry.hasSongLabel())
                .toList();
        if (unlabelled.isEmpty()) return;

        System.out.printf("%n  %d song(s) have no SONG LABEL and print their full title instead:%n", unlabelled.size());
        unlabelled.forEach(entry ->
                System.out.printf("    %s  %s%n", entry.getSet(), entry.getTitle()));
    }
}
