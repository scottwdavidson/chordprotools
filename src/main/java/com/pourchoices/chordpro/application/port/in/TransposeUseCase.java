package com.pourchoices.chordpro.application.port.in;

public interface TransposeUseCase {

    /**
     * Transposes a ChordPro file to a new key.
     *
     * @param inputPath       path to the source {@code .cho} file; must contain
     *                        a {@code {key:}} directive
     * @param offsetSemitones semitones to transpose (positive = up, negative = down)
     * @param outputPath      path to write the transposed file; must differ
     *                        from {@code inputPath} — the input is never overwritten
     */
    void transpose(String inputPath, int offsetSemitones, String outputPath);
}
