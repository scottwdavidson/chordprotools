package com.pourchoices.chordpro.adapter.in.file;

import com.pourchoices.chordpro.application.domain.model.GigSummary;
import com.pourchoices.chordpro.application.port.in.ListGigsUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;

import java.util.List;

/**
 * CLI command that lists every gig in {@code gigs.csv} with a song count.
 * Handy quick reference before running {@code copy-gig} or {@code export-setlist}.
 *
 * <pre>
 *   ./list-gigs
 * </pre>
 */
@Component
@Command(
        name = "list-gigs",
        description = "Lists every gig in gigs.csv with the number of songs assigned to each."
)
@Slf4j
public class ListGigsCommand implements Runnable {

    private static final String ROW_FMT = "%-40s  %s%n";

    private final ListGigsUseCase useCase;

    public ListGigsCommand(ListGigsUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    public void run() {
        List<GigSummary> gigs = useCase.listGigs();

        if (gigs.isEmpty()) {
            System.out.println("No gigs found in gigs.csv");
            return;
        }

        System.out.println();
        System.out.printf(ROW_FMT, "GIG", "SONGS");
        System.out.println("-".repeat(50));
        for (GigSummary g : gigs) {
            System.out.printf(ROW_FMT, g.getGig(), g.getSongCount());
        }
        System.out.println();
    }
}
