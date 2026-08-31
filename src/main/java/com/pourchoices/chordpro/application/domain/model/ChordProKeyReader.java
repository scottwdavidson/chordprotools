package com.pourchoices.chordpro.application.domain.model;

/**
 * Finds and parses the {@code {key:}} directive from an already-parsed
 * ChordPro header.
 *
 * <p>Single source of truth for "how do we find the song's key" — every
 * service that needs to know a song's key before doing something with it
 * (transposing it, deriving a key-variant filename, comparing it against a
 * sibling variant, …) should call this instead of re-implementing the
 * find-the-KEY-line-or-fail logic.
 *
 * <p>This is a utility class — it must not be instantiated.
 */
public final class ChordProKeyReader {

    private ChordProKeyReader() {}

    /**
     * Finds the {@code {key:}} directive in {@code header} and parses it.
     *
     * @param header            the already-parsed ChordPro header
     * @param sourceDescription identifies the source in the error message
     *                          (typically the file path), so failures are
     *                          actionable
     * @return the parsed {@link MusicalKey}
     * @throws IllegalArgumentException if no {@code {key:}} directive is present
     */
    public static MusicalKey readKey(ParsedHeader header, String sourceDescription) {
        return MusicalKey.parse(readRawKeyValue(header, sourceDescription));
    }

    /**
     * Finds the {@code {key:}} directive in {@code header} and returns its
     * value exactly as written (no re-spelling/normalization) — useful for
     * fidelity in human-readable output.
     *
     * @param header            the already-parsed ChordPro header
     * @param sourceDescription identifies the source in the error message
     *                          (typically the file path), so failures are
     *                          actionable
     * @return the raw {@code {key:}} value
     * @throws IllegalArgumentException if no {@code {key:}} directive is present
     */
    public static String readRawKeyValue(ParsedHeader header, String sourceDescription) {
        return findKeyLine(header, sourceDescription).getValue();
    }

    private static ParsedHeaderLine findKeyLine(ParsedHeader header, String sourceDescription) {
        return header.getHeaderLines().stream()
                .filter(l -> l.getHeaderDirective() == HeaderDirective.KEY)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No {key:} directive found in " + sourceDescription
                        + " - the song's key must be known before this operation can proceed."));
    }
}
