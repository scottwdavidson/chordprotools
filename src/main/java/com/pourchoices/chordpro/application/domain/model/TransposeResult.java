package com.pourchoices.chordpro.application.domain.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

import java.util.List;

/**
 * Structured outcome of a
 * {@link com.pourchoices.chordpro.application.port.in.TransposeUseCase#transpose}
 * call.
 *
 * <p>Carries everything an adapter (CLI command, or a future orchestrating
 * service) needs to report what happened. The service itself never prints —
 * see the hexagonal boundary notes in {@code command-reference.md}:
 * presentation belongs to {@code adapter/in}, not {@code application/domain}.
 */
@Value
@Builder
public class TransposeResult {

    @NonNull String inputPath;
    @NonNull String outputPath;

    /** The {@code {key:}} value exactly as written in the source file. */
    @NonNull String sourceKeyRaw;

    @NonNull MusicalKey sourceKey;
    @NonNull MusicalKey targetKey;

    /** Semitones the source was transposed by (positive = up, negative = down). */
    int offsetSemitones;

    /**
     * Body lines that looked like a chord attempt but weren't recognized
     * (left untransposed) — see {@code ChordProTransposer.findUnrecognizedChordAttempts}.
     * Empty when nothing suspicious was found. The adapter decides whether
     * and how to surface these (e.g. to {@code System.err}).
     */
    @Singular
    List<String> warnings;

    /** The target key's canonical, correctly-spelled name, e.g. {@code "Bb"}. */
    public String targetKeyName() {
        return targetKey.canonicalName();
    }
}
