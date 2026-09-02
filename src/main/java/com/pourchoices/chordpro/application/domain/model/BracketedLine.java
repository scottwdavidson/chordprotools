package com.pourchoices.chordpro.application.domain.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

import java.util.List;

/**
 * Result of {@link com.pourchoices.chordpro.application.domain.service.ChordProTransposer#bracketBareChords}
 * for a single line: the line with any bare (unbracketed) chord-shaped
 * tokens wrapped in {@code [ ]}, plus the exact tokens that were wrapped so
 * callers can report what changed without re-diffing text.
 */
@Value
@Builder
public class BracketedLine {

    @NonNull String line;

    /** The bare tokens (without brackets) that were recognized and wrapped. Empty if nothing changed. */
    @Singular
    List<String> wrappedTokens;

    public boolean changed() {
        return !wrappedTokens.isEmpty();
    }
}
