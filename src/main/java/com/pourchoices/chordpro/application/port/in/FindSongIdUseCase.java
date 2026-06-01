package com.pourchoices.chordpro.application.port.in;

import com.pourchoices.chordpro.application.domain.model.SongMatch;

import java.util.List;

/**
 * Input port for {@code find-song-id}: search the song catalog by a
 * title-or-artist fragment and return one result per song (base version),
 * annotated with key-variant counts.
 *
 * @see com.pourchoices.chordpro.application.domain.service.FindSongIdService
 */
public interface FindSongIdUseCase {

    /**
     * Finds songs whose title or artist contains the given fragment
     * (case-insensitive), grouped so each song appears once with its
     * variant count.
     *
     * @param fragment title or artist substring to match (case-insensitive)
     * @return matches sorted by title (case-insensitive); empty if none match
     */
    List<SongMatch> findByFragment(String fragment);
}
