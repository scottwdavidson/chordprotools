package com.pourchoices.chordpro.application.domain.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

import java.util.List;

/**
 * Structured outcome of an
 * {@link com.pourchoices.chordpro.application.port.in.ImportNewSongUseCase#importNewSong}
 * call.
 *
 * <p>Carries everything an adapter (CLI command, or a future orchestrating
 * service) needs to report what happened. The service itself never prints —
 * see the hexagonal boundary notes in {@code command-reference.md}:
 * presentation belongs to {@code adapter/in}, not {@code application/domain}.
 */
@Value
@Builder
public class ImportResult {

    /** The entry that was appended (real run) or would be appended (dry run). */
    @NonNull CatalogEntry catalogEntry;

    /** {@code true} when nothing was actually written to song-catalog.csv. */
    boolean dryRun;

    /**
     * Total catalog entry count after this import — for a dry run, the
     * count as it stands today (nothing changed yet); for a real run, the
     * count including the newly-appended entry.
     */
    int catalogSizeAfter;

    /**
     * House-style warnings about the imported entry (title not initial caps, artist that looks
     * like a collaboration credit or a near-duplicate of an existing artist). Advisory only -
     * the song was still imported.
     */
    @Singular("warning")
    List<String> warnings;
}
