package com.pourchoices.chordpro.application.port.in;

import com.pourchoices.chordpro.application.domain.model.BracketChordsReport;

/**
 * Scans every instrumental-notation line (any line with a {@code |} measure
 * separator) across the whole catalog for bare (unbracketed) chords, repeat
 * shorthand, and strum-slash notation.
 *
 * <p>Bare chords are safely auto-fixable — see
 * {@link com.pourchoices.chordpro.application.domain.service.ChordProTransposer#bracketBareChords}
 * — and get wrapped in {@code [ ]} when {@code fix} is true. Repeat shorthand
 * and strum-slash notation are reported only; both need a human decision
 * (how many times to expand a repeat, what a stray slash was meant to be).
 */
public interface BracketChordsUseCase {

    /**
     * @param fix when {@code true}, rewrite files to wrap bare chords in
     *            brackets; when {@code false} (default), report only.
     * @return the report; all three finding types are always populated
     *         regardless of {@code fix}, so drift is visible either way.
     */
    BracketChordsReport run(boolean fix);
}
