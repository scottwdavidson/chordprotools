package com.pourchoices.chordpro.application.port.in;

import com.pourchoices.chordpro.application.domain.model.SemanticDiffReport;
import com.pourchoices.chordpro.application.domain.model.SongId;

import java.util.List;

public interface ConsistentSongDataUseCase {

    /**
     * Checks that every key-variant of a song is semantically identical
     * (lyrics + harmony) to the group's reference (base/standard-key)
     * variant, once pure transposition and enharmonic respelling are
     * accounted for. Detection only — no {@code --fix} in this phase.
     *
     * @param songId identifies the song group (the key-variant suffix, if
     *               any, is ignored — the whole group is checked)
     * @return one {@link SemanticDiffReport} per non-reference variant,
     *         reference always as {@code fileA}; empty if the group has
     *         fewer than 2 variants (nothing to compare)
     * @throws IllegalArgumentException if no catalog entries exist for the
     *                                  song's group
     */
    List<SemanticDiffReport> check(SongId songId);
}
