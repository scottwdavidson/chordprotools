package com.pourchoices.chordpro.adapter.in.file;

import com.pourchoices.chordpro.application.domain.model.Setlist;
import com.pourchoices.chordpro.application.port.in.PrintFanSetlistUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * CLI command that writes the fans' setlist handout for the gig: the band, the venue and date,
 * then each set's songs by title and artist. Open the result in a browser and print it (or save
 * it as a PDF to send) — a short setlist is one page, a full gig about two.
 *
 * <pre>
 *   # latest gig → ./work/fan-setlist.html (git-ignored, overwritten each run)
 *   chordpro-tools print-fan-setlist
 *
 *   # specific gig, custom output
 *   chordpro-tools print-fan-setlist --gig 2026-06-14-rusty-nail --output ./rusty-nail-fans.html
 * </pre>
 */
@Component
@Command(
        name = "print-fan-setlist",
        description = "Writes a printable setlist handout for fans (title and artist per song, grouped "
                + "by set) as HTML. Backup / Z-set songs are never included."
)
@Slf4j
public class PrintFanSetlistCommand implements Runnable {

    /** Under the git-ignored work/ directory, so the sheet is never committed and just gets overwritten. */
    private static final String DEFAULT_OUTPUT = "./work/fan-setlist.html";

    private final PrintFanSetlistUseCase printFanSetlistUseCase;

    public PrintFanSetlistCommand(PrintFanSetlistUseCase printFanSetlistUseCase) {
        this.printFanSetlistUseCase = printFanSetlistUseCase;
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

    @Override
    public void run() {
        log.info("Printing fan setlist (gig={}) to: {}", gig, outputPath);

        Setlist setlist = printFanSetlistUseCase.printFanSetlist(gig, outputPath);

        System.out.printf("%nFan setlist ready — %d songs for gig '%s' written to %s%n",
                setlist.size(), setlist.getGig(), outputPath);
        System.out.println("  Open it in a browser and print: Portrait, Letter, margins Default, headers/footers off.");
        System.out.println();
    }
}
