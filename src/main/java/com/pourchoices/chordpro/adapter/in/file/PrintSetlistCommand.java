package com.pourchoices.chordpro.adapter.in.file;

import com.pourchoices.chordpro.application.domain.model.Setlist;
import com.pourchoices.chordpro.application.port.in.PrintSetlistUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * CLI command that writes a printable, two-page (portrait) setlist for the gig showing only
 * SET, TITLE and BACKING. Open the result in a browser and print it for the stage.
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
        description = "Writes a printable two-page portrait setlist (set, title, backing) as HTML. "
                + "Backup / Z-set songs are never included."
)
@Slf4j
public class PrintSetlistCommand implements Runnable {

    private static final String DEFAULT_OUTPUT = "./setlist.html";

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

    @Override
    public void run() {
        log.info("Printing setlist (gig={}) to: {}", gig, outputPath);

        Setlist setlist = printSetlistUseCase.printSetlist(gig, outputPath);

        System.out.printf("%nPrintable setlist ready — %d songs for gig '%s' written to %s%n",
                setlist.size(), setlist.getGig(), outputPath);
        System.out.println("  Open it in a browser and print: Portrait, Letter, margins Default, headers/footers off.");
        System.out.println();
    }
}
