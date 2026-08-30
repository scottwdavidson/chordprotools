package com.pourchoices.chordpro.application.domain.model;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

/**
 * Immutable result of a {@code verify-sync} run comparing two {@code .cho}
 * files for semantic drift — differences that survive stripping out pure
 * transposition and enharmonic respelling.
 *
 * <p>Mirrors {@link MetadataConsistencyReport}'s shape (findings list +
 * summary, exit-code-friendly {@link #issueCount()}) for consistency across
 * report-style commands.
 */
@Value
@Builder
public class SemanticDiffReport {

    /** The kind of drift found between the two files. */
    public enum FindingType {
        /** Lyric/body text differs once chords and directives are stripped. */
        LYRIC_DESYNC,
        /** The chord at this position maps to a different scale degree in
         *  each file, relative to that file's own {@code {key:}}. */
        HARMONIC_DRIFT
    }

    /** One drift finding at a specific body line. */
    @Value
    @Builder
    public static class Finding {
        FindingType type;
        /** 1-based body line number the drift was found at. */
        int lineNumber;
        /** Human-readable detail, already formatted. */
        String detail;
    }

    String fileA;
    String fileB;

    @Singular
    List<Finding> findings;

    /** Number of findings — drives the scriptable exit code. */
    public int issueCount() {
        return findings.size();
    }
}
