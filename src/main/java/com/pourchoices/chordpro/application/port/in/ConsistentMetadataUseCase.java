package com.pourchoices.chordpro.application.port.in;

import com.pourchoices.chordpro.application.domain.model.MetadataConsistencyReport;

/**
 * Checks that key-variants of the same song share consistent catalog metadata.
 *
 * <p>Scans the whole catalog. Two checks per song group:
 * <ol>
 *   <li><b>DRIFT</b> — any field other than KEY differs between variants
 *       (including PERFORMANCE KEY, which must always match — it's the sounding
 *       key everyone plays in).</li>
 *   <li><b>FILENAME/KEY</b> — a variant whose filename encodes a key (e.g.
 *       {@code -bb}) must have a {@code {key:}} that matches that suffix
 *       (enharmonic-aware). Report-only; never auto-fixed.</li>
 * </ol>
 */
public interface ConsistentMetadataUseCase {

    /**
     * Runs the consistency check.
     *
     * @param fix     when {@code true}, repair DRIFT by propagating metadata
     *                from the source-of-truth variant into the catalog;
     *                when {@code false} (default), report only.
     * @param sourceSongId the song ID to treat as the source of truth when
     *                {@code fix} is true; {@code null} means "use the base
     *                (standard-key) variant of each group".
     * @return the report (its {@link MetadataConsistencyReport#issueCount()}
     *                drives the shell exit code)
     */
    MetadataConsistencyReport check(boolean fix, String sourceSongId);
}
