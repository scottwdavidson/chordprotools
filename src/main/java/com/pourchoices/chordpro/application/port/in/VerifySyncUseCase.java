package com.pourchoices.chordpro.application.port.in;

import com.pourchoices.chordpro.application.domain.model.SemanticDiffReport;

public interface VerifySyncUseCase {

    /**
     * Compares two {@code .cho} files for semantic drift — differences that
     * survive stripping out pure transposition and enharmonic respelling.
     * Deliberately catalog-agnostic: takes two raw file paths, no
     * {@code SongId} knowledge required.
     *
     * @param fileAPath path to the first {@code .cho} file
     * @param fileBPath path to the second {@code .cho} file
     * @return the drift report; {@link SemanticDiffReport#issueCount()} is 0
     *         when the files are semantically in sync
     */
    SemanticDiffReport verifySync(String fileAPath, String fileBPath);
}
