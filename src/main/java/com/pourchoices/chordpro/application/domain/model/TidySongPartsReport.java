package com.pourchoices.chordpro.application.domain.model;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

/**
 * Immutable result of a {@code tidy-song-parts} run.
 *
 * <p>Two independent transformations, both always applied together:
 * <ol>
 *   <li><b>BRIDGE_DIRECTIVE</b> — {@code {start_of_bridge[: Label]}} /
 *       {@code {end_of_bridge}} rewritten to {@code {start_of_part: Label}} /
 *       {@code {end_of_part}}. OnSong doesn't render the dedicated bridge
 *       directive correctly, so the generic part directive is used for
 *       everything instead. Pure 1:1 token substitution - always safe.</li>
 *   <li><b>COMMENT_HEADER</b> — a {@code {comment: ...}} line whose text
 *       contains "Intro", "Bridge", "Solo", "Instrumental", or "Outro" as a
 *       whole word gets wrapped in {@code {start_of_part: <original text>}}
 *       / {@code {end_of_part}}, bounding the instrumental-notation lines
 *       that follow until the next structural directive, the next matching
 *       comment, or end of file.</li>
 * </ol>
 *
 * <p>Every {@code COMMENT_HEADER} finding also records whether the keyword
 * led the comment text ({@code LEADING}, e.g. "Intro") or was found deeper
 * in a longer phrase ({@code EMBEDDED}, e.g. "Guitar Solo", "Scott Solo on
 * VERSE &amp; CHORUS"). Both are fixed identically under {@code --fix} - the
 * split exists purely so the report makes the lower-confidence matches easy
 * to skim before trusting a big catalog-wide fix.
 */
@Value
@Builder
public class TidySongPartsReport {

    public enum FindingType {
        BRIDGE_DIRECTIVE,
        COMMENT_HEADER
    }

    public enum MatchConfidence {
        /** Keyword is the first word of the comment text, e.g. "Intro". */
        LEADING,
        /** Keyword appears elsewhere in the text, e.g. "Guitar Solo". */
        EMBEDDED,
        /** N/A - only COMMENT_HEADER findings carry a confidence. */
        NONE
    }

    @Value
    @Builder
    public static class Finding {
        FindingType type;
        MatchConfidence confidence;
        String songId;
        String filePath;
        int lineNumber;
        String label;
        /** Number of content lines wrapped between start/end - COMMENT_HEADER only. */
        int contentLineCount;
    }

    @Singular
    List<Finding> findings;

    int filesScanned;
    int filesChanged;

    public long countByType(FindingType type) {
        return findings.stream().filter(f -> f.getType() == type).count();
    }

    public long countEmptyParts() {
        return findings.stream()
                .filter(f -> f.getType() == FindingType.COMMENT_HEADER)
                .filter(f -> f.getContentLineCount() == 0)
                .count();
    }
}
