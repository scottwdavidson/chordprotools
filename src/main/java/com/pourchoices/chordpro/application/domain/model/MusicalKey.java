package com.pourchoices.chordpro.application.domain.model;

import lombok.Value;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A musical key — a root note plus a major/minor quality.
 *
 * <p>The whole point of this value object is to define <b>key equality</b> in
 * exactly one place, including <b>enharmonic equivalence</b>: {@code A#} and
 * {@code Bb} are the same key, {@code C#m} and {@code Dbm} are the same key,
 * while {@code C} and {@code Cm} are <em>not</em> (different quality).
 *
 * <p>Equality is defined by the {@link #chromaticPosition} (0–11, where C = 0)
 * and the {@link #minor} flag — never by the original spelling. Two keys parsed
 * from different spellings of the same pitch compare equal.
 *
 * <p>Accepted spellings (case-insensitive root letter):
 * <pre>
 *   C   c   →  C major
 *   Bb  bb  →  B-flat major
 *   F#  f#  →  F-sharp major
 *   Am  am  →  A minor
 *   C#m c#m →  C-sharp minor
 * </pre>
 */
@Value
public class MusicalKey {

    /** Root letter, optional single accidental (# or b), optional trailing m. */
    private static final Pattern KEY_PATTERN =
            Pattern.compile("^([A-Ga-g])([#b]?)(m?)$");

    /** Semitone offset of each natural note from C. */
    private static int naturalPosition(char letter) {
        return switch (Character.toUpperCase(letter)) {
            case 'C' -> 0;
            case 'D' -> 2;
            case 'E' -> 4;
            case 'F' -> 5;
            case 'G' -> 7;
            case 'A' -> 9;
            case 'B' -> 11;
            default  -> throw new IllegalArgumentException("Not a note letter: " + letter);
        };
    }

    /** Chromatic position 0–11 (C = 0). Defines enharmonic equality. */
    int chromaticPosition;

    /** {@code true} for a minor key, {@code false} for major. */
    boolean minor;

    /**
     * Parses a key string into a {@link MusicalKey}.
     *
     * @param key e.g. {@code "C"}, {@code "Bb"}, {@code "F#m"}, {@code "am"}
     * @return the parsed key
     * @throws IllegalArgumentException if {@code key} is blank or unparseable
     */
    public static MusicalKey parse(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key must not be blank");
        }
        Matcher m = KEY_PATTERN.matcher(key.trim());
        if (!m.matches()) {
            throw new IllegalArgumentException("Unparseable musical key: \"" + key + "\"");
        }

        int position = naturalPosition(m.group(1).charAt(0));
        String accidental = m.group(2);
        if (accidental.equals("#")) {
            position = Math.floorMod(position + 1, 12);
        } else if (accidental.equals("b")) {
            position = Math.floorMod(position - 1, 12);
        }
        boolean minor = !m.group(3).isEmpty();

        return new MusicalKey(position, minor);
    }

    /**
     * Returns {@code true} if {@code key} can be parsed as a musical key.
     * Useful for guarding before calling {@link #parse(String)}.
     */
    public static boolean isParseable(String key) {
        return key != null && KEY_PATTERN.matcher(key.trim()).matches();
    }
}
