package com.pourchoices.chordpro.application.port.in;

/**
 * Verifies that every entry in {@code song-catalog.csv} is consistent with
 * its corresponding {@code .cho} file on disk.
 *
 * @return the number of entries with issues (0 = everything is clean)
 */
public interface VerifyCatalogUseCase {

    int verifyCatalog();
}
