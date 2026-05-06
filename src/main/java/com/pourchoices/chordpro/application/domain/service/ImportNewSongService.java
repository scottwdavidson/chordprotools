package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.port.in.ImportNewSongUseCase;
import org.springframework.stereotype.Service;

/**
 * Placeholder — import-new-song is not yet implemented.
 *
 * When implemented this service will:
 *   1. Parse the supplied .cho file into a ParsedSong
 *   2. Derive a CatalogEntry from the parsed header
 *   3. Append the new entry to the song-catalog.csv via CatalogPort
 */
@Service
public class ImportNewSongService implements ImportNewSongUseCase {

    @Override
    public void importNewSong(String chordproSongPathString) {
        throw new UnsupportedOperationException(
                "import-new-song is not yet implemented: " + chordproSongPathString);
    }
}
