package com.pourchoices.chordpro.application.port.in;

import com.pourchoices.chordpro.application.domain.model.CharacterizeSongsReport;

public interface CharacterizeSongsUseCase {

    /**
     * @param ratingsInputPath path to the ratings CSV (see {@code SongRating})
     * @param fix               if false (default), report only — song-catalog.csv is never written
     */
    CharacterizeSongsReport characterizeSongs(String ratingsInputPath, boolean fix);
}
