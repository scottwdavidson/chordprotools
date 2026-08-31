package com.pourchoices.chordpro.application.domain.model;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

/**
 * Structured outcome of {@link com.pourchoices.chordpro.application.port.in.TidyGigsUseCase#tidyGigs}.
 * {@code TidyGigsService} never prints directly - {@code TidyGigsCommand}
 * owns all presentation, same convention as {@code TransposeResult}/
 * {@code ImportResult}.
 */
@Value
@Builder
public class TidyGigsResult {

    /** {@code true} when gigs.csv doesn't exist or has no data rows - a no-op, not a failure. */
    boolean fileMissingOrEmpty;

    /** Line numbers (1-based) auto-repaired for a missing trailing column, in file order. */
    @Singular
    List<Integer> repairedLineNumbers;

    /** Rows with MORE fields than the header - left untouched, need human review. */
    @Singular
    List<GigsRowRepair.RejectedRow> rejectedRows;

    /** {@code true} once the file was actually re-sorted and written back. */
    boolean tidied;

    /** {@code false} only when {@link #getRejectedRows()} is non-empty - the file was left untouched. */
    public boolean isSuccessful() {
        return rejectedRows.isEmpty();
    }

    public boolean hasRepairs() {
        return !repairedLineNumbers.isEmpty();
    }
}
