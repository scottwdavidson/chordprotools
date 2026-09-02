package com.pourchoices.chordpro.application.domain.model;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

/**
 * Immutable result of a {@code bracket-chords} run.
 *
 * <p>Scans instrumental-notation lines (any line containing a {@code |}
 * measure separator) across the whole catalog for three distinct issues:
 * <ol>
 *   <li><b>BARE_CHORD</b> — a chord-shaped token with no surrounding
 *       brackets, invisible to {@code transpose}. Safely auto-fixable
 *       (see {@link com.pourchoices.chordpro.application.domain.service.ChordProTransposer#bracketBareChords});
 *       fixed when {@code --fix} is given.</li>
 *   <li><b>REPEAT_SHORTHAND</b> — {@code :||}, {@code xN}, or "repeat"
 *       notation. Never auto-fixed: shortens the line count OnSong scrolls
 *       through, which needs a human to expand to the real measure count
 *       (and "repeat and fade" has no fixed count at all).</li>
 *   <li><b>STRUM_SLASH</b> — bare {@code /} rhythm-slash tokens. Never
 *       auto-fixed: unclear what (if anything) they're supposed to
 *       represent; needs a human to look at the source.</li>
 * </ol>
 */
@Value
@Builder
public class BracketChordsReport {

    public enum FindingType {
        BARE_CHORD,
        REPEAT_SHORTHAND,
        STRUM_SLASH
    }

    @Value
    @Builder
    public static class Finding {
        FindingType type;
        String songId;
        String filePath;
        int lineNumber;
        String originalLine;
        /** Only populated for BARE_CHORD findings when {@code --fix} was applied. */
        String fixedLine;
    }

    @Singular
    List<Finding> findings;

    int filesScanned;

    /** Files with at least one bare chord - fixed on disk only when --fix was given. */
    int filesWithBareChords;

    public long countByType(FindingType type) {
        return findings.stream().filter(f -> f.getType() == type).count();
    }
}
