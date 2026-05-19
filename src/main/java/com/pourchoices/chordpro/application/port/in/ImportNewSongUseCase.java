package com.pourchoices.chordpro.application.port.in;

public interface ImportNewSongUseCase {

    /**
     * Imports a new song into the catalog from its {@code .cho} file.
     *
     * @param chordproSongPathString path to the {@code .cho} file
     * @param dryRun                 when {@code true}, prints what would be added
     *                               without modifying {@code song-catalog.csv}
     */
    void importNewSong(String chordproSongPathString, boolean dryRun);
}
