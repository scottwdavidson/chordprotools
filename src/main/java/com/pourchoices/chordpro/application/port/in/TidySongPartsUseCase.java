package com.pourchoices.chordpro.application.port.in;

import com.pourchoices.chordpro.application.domain.model.TidySongPartsReport;

/**
 * Converts two kinds of ad-hoc section markers into proper
 * {@code {start_of_part}}/{@code {end_of_part}} directive pairs across the
 * whole catalog:
 *
 * <ul>
 *   <li>{@code {start_of_bridge}}/{@code {end_of_bridge}} - OnSong doesn't
 *       render these correctly, so they're rewritten to the generic part
 *       directive, preserving any label.</li>
 *   <li>{@code {comment: Intro}} (and "Bridge"/"Solo"/"Instrumental"/"Outro"/
 *       "Pre-Chorus" variants, however worded) - these are currently just a
 *       floating comment with no explicit end, relying on the next
 *       {@code {start_of_verse}} etc. to implicitly close them. They get
 *       wrapped in a real part directive pair instead.</li>
 * </ul>
 */
public interface TidySongPartsUseCase {

    /**
     * @param fix when {@code true}, rewrite files; when {@code false}
     *            (default), report only.
     * @return the report; always fully populated regardless of {@code fix}.
     */
    TidySongPartsReport run(boolean fix);
}
