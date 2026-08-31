package com.pourchoices.chordpro.adapter.in.file;

import com.pourchoices.chordpro.application.domain.model.GigsRowRepair;
import com.pourchoices.chordpro.application.domain.model.TidyGigsResult;
import com.pourchoices.chordpro.application.port.in.TidyGigsUseCase;
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
        TidyGigsResult result = tidyGigsUseCase.tidyGigs();
        print(result);
        return result.isSuccessful() ? 0 : 1;
    }

    private void print(TidyGigsResult result) {
        if (result.isFileMissingOrEmpty()) {
            System.out.println("gigs.csv is empty or not found. Nothing to do.");
            return;
        }

        if (!result.isSuccessful()) {
            System.err.printf("ERROR: gigs.csv has %d row(s) with MORE fields than the header - "
                            + "can't safely guess how to repair these. Refusing to touch the file.%n",
                    result.getRejectedRows().size());
            for (GigsRowRepair.RejectedRow row : result.getRejectedRows()) {
                System.err.printf("  line %d: %s%n", row.getLineNumber(), row.getRawContent());
            }
            System.err.println("Fix these row(s) by hand, then re-run ./tidy-gigs.");
            return;
        }

        if (result.hasRepairs()) {
            System.out.printf(
                    "Repaired %d row(s) missing trailing column(s) (assumed blank), at line(s): %s%n",
                    result.getRepairedLineNumbers().size(), result.getRepairedLineNumbers());
        }

        System.out.println("Tidied and sorted gigs.csv (by GIG and SET).");
    }
}
