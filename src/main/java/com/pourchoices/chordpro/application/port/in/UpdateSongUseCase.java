package com.pourchoices.chordpro.application.port.in;

import com.pourchoices.chordpro.application.domain.model.SongId;

public interface UpdateSongUseCase {

    /**
     * Updates a song from the catalog, identified by its {@link SongId}.
     *
     * <p>Because song metadata (duration, count-in, etc.) is shared across all
     * key-variants of a song, a single invocation fans out to the base file
     * <em>and</em> every key-variant in the same song group.
     *
     * @param songId identity of the song to update (variant suffix ignored for
     *               grouping — the whole group is updated)
     */
    void updateSong(SongId songId);

}
