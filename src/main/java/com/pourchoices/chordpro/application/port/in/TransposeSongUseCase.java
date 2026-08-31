package com.pourchoices.chordpro.application.port.in;

import com.pourchoices.chordpro.application.domain.model.TransposeSongResult;

public interface TransposeSongUseCase {

    /**
     * Creates a new key-variant of a song: resolves the source {@code .cho}
     * file from a SONG ID, transposes it by {@code offsetSemitones}, derives
     * the target SONG ID/path from the naming convention automatically, and
     * (unless {@code skipImport}) registers the result in
     * {@code song-catalog.csv}.
     *
     * @param sourceSongId    the SONG ID to transpose from, e.g.
     *                        {@code "ABC:B:BobSeger:HollywoodNights"} or an
     *                        existing key-variant like
     *                        {@code "ABC:B:BobSeger:HollywoodNights-b"}
     * @param offsetSemitones semitones to transpose (positive = up, negative = down)
     * @param skipImport      when {@code true}, writes the {@code .cho} file
     *                        but never touches {@code song-catalog.csv}
     * @return a structured summary of both steps; the caller (CLI adapter)
     *         decides how to present it
     */
    TransposeSongResult transposeSong(String sourceSongId, int offsetSemitones, boolean skipImport);
}
