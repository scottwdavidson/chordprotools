package com.pourchoices.chordpro.application.port.in;

import com.pourchoices.chordpro.application.domain.model.ImportResult;

public interface ImportNewSongUseCase {

    /**
     * Imports a new song into the catalog from its {@code .cho} file.
     *
     * @param chordproSongPathString path to the {@code .cho} file
     * @param dryRun                 when {@code true}, computes and returns
     *                               what would be added without modifying
     *                               {@code song-catalog.csv}
     * @return a structured summary of the (would-be) catalog entry; the
     *         caller (CLI adapter, or an orchestrating service) decides how,
     *         or whether, to present it
     */
    ImportResult importNewSong(String chordproSongPathString, boolean dryRun);
}
