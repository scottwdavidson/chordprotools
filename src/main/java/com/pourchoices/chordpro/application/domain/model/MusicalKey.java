package com.pourchoices.chordpro.application.domain.model;

import lombok.Value;

import java.util.Set;
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

    /**
     * Major-key chromatic positions that prefer flat spelling (F, Bb, Eb, Ab,
     * Db). Circle-of-fifths flat side, per band convention.
     *
     * <p><b>Tie-break:</b> position 6 (F#/Gb) is deliberately excluded here and
     * defaults to sharp ({@code F#}) instead of flat ({@code Gb}) — both are
     * valid enharmonic spellings of the same key in real music theory, and
     * F# is the far more common spelling in contemporary rock/pop guitar
     * charts (this band's repertoire).
     */
    private static final Set<Integer> FLAT_MAJOR_POSITIONS = Set.of(5, 10, 3, 8, 1);

    /**
     * Minor-key chromatic positions that prefer flat spelling (Dm, Gm, Cm,
     * Fm, Bbm, Ebm).
     *
     * <p><b>Tie-break:</b> position 3 (D#m/Ebm) is included here — Eb minor
     * is the far more common practical spelling versus D# minor, which is
     * essentially never used in real charts.
     */
    private static final Set<Integer> FLAT_MINOR_POSITIONS = Set.of(2, 7, 0, 5, 10, 3);

    /**
     * Whether this key should be spelled using flats (as opposed to sharps)
     * when rendering a transposed chord. Neutral keys (C major, A minor)
     * fall through to sharps by default, matching {@link #FLAT_MAJOR_POSITIONS}
     * / {@link #FLAT_MINOR_POSITIONS} not containing their positions.
     *
     * <p>Two chromatic positions (major position 6, minor position 3) are
     * genuinely ambiguous in real music theory — both a flat and a sharp
     * spelling are valid, commonly-used names for the same key. See the
     * tie-break notes on {@link #FLAT_MAJOR_POSITIONS} / {@link #FLAT_MINOR_POSITIONS}
     * for the deliberate, documented choice made here.
     */
    public boolean prefersFlats() {
        return minor
                ? FLAT_MINOR_POSITIONS.contains(chromaticPosition)
                : FLAT_MAJOR_POSITIONS.contains(chromaticPosition);
    }

    /**
     * Note names indexed by chromatic position (0-11, C = 0), sharp spelling.
     * The single source of truth for pitch-class names — {@link
     * com.pourchoices.chordpro.application.domain.service.ChordProTransposer}
     * reuses this instead of keeping its own copy.
     */
    private static final String[] SHARP_NAMES = {
            "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"
    };

    /** Note names indexed by chromatic position (0-11, C = 0), flat spelling. */
    private static final String[] FLAT_NAMES = {
            "C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B"
    };

    /**
     * Renders a chromatic position (0-11) as a note name, spelled with flats
     * or sharps as requested. {@code chromaticPosition} is normalized with
     * {@link Math#floorMod} so callers can pass any integer, not just 0-11.
     */
    public static String noteName(int chromaticPosition, boolean useFlats) {
        int position = Math.floorMod(chromaticPosition, 12);
        return (useFlats ? FLAT_NAMES : SHARP_NAMES)[position];
    }

    /**
     * Renders this key's canonical name for display / writing back into a
     * {@code {key:}} directive, e.g. {@code "F#"}, {@code "Bbm"}. Spelling
     * (flat vs sharp) is chosen via {@link #prefersFlats()}.
     */
    public String canonicalName() {
        return noteName(chromaticPosition, prefersFlats()) + (minor ? "m" : "");
    }

    /**
     * Returns the key reached by transposing this key up (or down, for a
     * negative value) by {@code halfSteps} semitones. Quality (major/minor)
     * is preserved — transposition never changes a major key into a minor
     * one or vice versa.
     */
    public MusicalKey transposeBy(int halfSteps) {
        return new MusicalKey(Math.floorMod(chromaticPosition + halfSteps, 12), minor);
    }

    /**
     * Roman-numeral scale-degree names, indexed by chromatic interval (0-11)
     * from this key's tonic. The 7 diatonic degrees use the exact
     * upper/lower-case + ° convention from the design spec (major: I, ii,
     * iii, IV, V, vi, vii°; natural minor: i, ii°, III, iv, v, VI, VII). The
     * other 5 chromatic positions per mode are a deliberate, documented
     * convention chosen for <b>internally consistent comparison</b> (used by
     * {@code SemanticDiffService} to detect harmonic drift) rather than a
     * claim of being the one true music-theory label: major spells
     * chromatic notes as "flat of the degree above" (bII, bIII, bV, bVI,
     * bVII — all standard borrowed-chord names in rock/pop), while minor
     * spells them as "sharp of the degree below" (matching how melodic/
     * harmonic minor actually raise the 6th and 7th degrees; position 11 as
     * "vii°" is the harmonic-minor leading-tone diminished chord, a real and
     * common minor-key chord, not an arbitrary chromatic guess).
     */
    private static final String[] MAJOR_ROMAN_NUMERALS = {
            "I", "bII", "ii", "bIII", "iii", "IV", "bV", "V", "bVI", "vi", "bVII", "vii\u00b0"
    };

    private static final String[] MINOR_ROMAN_NUMERALS = {
            "i", "bII", "ii\u00b0", "III", "#III", "iv", "#iv", "v", "VI", "#VI", "VII", "vii\u00b0"
    };

    /**
     * The Roman-numeral scale-degree name for a chromatic pitch relative to
     * this key, e.g. {@code C.romanNumeralDegree(7)} (G, the 5th) → {@code
     * "V"}. See {@link #MAJOR_ROMAN_NUMERALS} / {@link #MINOR_ROMAN_NUMERALS}
     * for the chromatic-position naming convention.
     */
    public String romanNumeralDegree(int chordRootChromaticPosition) {
        int interval = Math.floorMod(chordRootChromaticPosition - chromaticPosition, 12);
        return (minor ? MINOR_ROMAN_NUMERALS : MAJOR_ROMAN_NUMERALS)[interval];
    }
}
