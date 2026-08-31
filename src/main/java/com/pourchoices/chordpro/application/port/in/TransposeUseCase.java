package com.pourchoices.chordpro.application.port.in;

import com.pourchoices.chordpro.application.domain.model.TransposeResult;

public interface TransposeUseCase {

    /**
     * Transposes a ChordPro file to a new key.
     *
     * @param inputPath       path to the source {@code .cho} file; must contain
     *                        a {@code {key:}} directive
     * @param offsetSemitones semitones to transpose (positive = up, negative = down)
     * @param outputPath      path to write the transposed file; must differ
     *                        from {@code inputPath} — the input is never overwritten
     * @return a structured summary of what was transposed and to where; the
     *         caller (CLI adapter, or an orchestrating service) decides how,
     *         or whether, to present it
     */
    TransposeResult transpose(String inputPath, int offsetSemitones, String outputPath);
}
