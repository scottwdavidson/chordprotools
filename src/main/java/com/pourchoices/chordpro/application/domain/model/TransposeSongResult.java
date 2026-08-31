package com.pourchoices.chordpro.application.domain.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * Structured outcome of a
 * {@link com.pourchoices.chordpro.application.port.in.TransposeSongUseCase#transposeSong}
 * call — the composed "transpose, then catalog it" workflow.
 *
 * <p>The catalog step is deliberately never rolled back on failure: if
 * {@link #getImportFailureMessage()} is present, the transposed {@code .cho}
 * file at {@code transposeResult.getOutputPath()} was still written
 * successfully and is left in place. The adapter decides how to present
 * that (see {@code TransposeCommand}) — this service never prints.
 */
@Value
@Builder
public class TransposeSongResult {

    @NonNull SongId sourceSongId;
    @NonNull SongId targetSongId;
    @NonNull TransposeResult transposeResult;

    /** Present only when the catalog step actually ran and succeeded. */
    ImportResult importResult;

    /** {@code true} when {@code --no-import} was passed; the catalog step never ran. */
    boolean importSkipped;

    /**
     * Present only when the catalog step was attempted and threw — the
     * exception's message, for a human-readable report. {@code null} in
     * every other case (skipped or succeeded).
     */
    String importFailureMessage;

    /** {@code true} only when the catalog step ran and succeeded. */
    public boolean isImportSuccessful() {
        return importResult != null;
    }

    /** {@code true} when the catalog step ran but failed. */
    public boolean isImportFailed() {
        return importFailureMessage != null;
    }
}
