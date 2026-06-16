package com.pourchoices.chordprotools.adapter.in.cli;

import com.pourchoices.chordprotools.application.port.in.TidyGigsUseCase;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

@Component
@Command(name = "tidy-gigs", description = "Strips carriage returns and sorts gigs.csv by GIG, then SET")
public class TidyGigsCommand implements Callable<Integer> {

    private final TidyGigsUseCase tidyGigsUseCase;

    public TidyGigsCommand(TidyGigsUseCase tidyGigsUseCase) {
        this.tidyGigsUseCase = tidyGigsUseCase;
    }

    @Override
    public Integer call() {
        tidyGigsUseCase.tidyGigs();
        return 0;
    }
}