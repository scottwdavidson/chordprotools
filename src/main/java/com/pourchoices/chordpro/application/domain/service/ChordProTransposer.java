package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.MusicalKey;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Transposes ChordPro format music lines from one key to another.
 * ChordPro format: [C]Something something, [Fmaj7], something something[Gm/A]
 *
 * <p>Only text inside square brackets that actually parses as a chord gets
 * touched. Real {@code .cho} files in this catalog also use square brackets
 * for guitar-tab fragments, riff notation ({@code [E F# G A]}), section
 * labels, and the occasional typo — none of that should be mangled just
 * because it happens to start with a letter A-G. See {@link #isValidQuality}.
 */
@Service
public class ChordProTransposer {

    private static final String[] CHROMATIC_SCALE_SHARPS = {
            "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"
    };

    private static final String[] CHROMATIC_SCALE_FLATS = {
            "C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B"
    };

    // Enharmonic equivalents normalized to a standard note before re-spelling.
    private static final Map<String, String> ENHARMONIC_MAP = new HashMap<>();
    static {
        ENHARMONIC_MAP.put("E#", "F");
        ENHARMONIC_MAP.put("B#", "C");
        ENHARMONIC_MAP.put("Cb", "B");
        ENHARMONIC_MAP.put("Fb", "E");
    }

    /**
     * Matches a whole bracketed chord token: root, quality/extension text,
     * and an optional slash bass note. Deliberately excludes '/' from the
     * quality group — a chord like "C6/9" is a known, documented limitation
     * (see the design doc); it won't be recognized as a single chord and is
     * left untouched rather than risk mangling it.
     */
    private static final Pattern CHORD_PATTERN = Pattern.compile(
            "\\[([A-G][#b]*)([^/\\]]*)(?:/([A-G][#b]*))?]",
            Pattern.CASE_INSENSITIVE);

    /**
     * Validates the "quality/extension" text captured between the root note
     * and the bass note or closing bracket. Built from the actual chord
     * suffixes found across this catalog's {@code .cho} files (m, maj7, m7,
     * sus2/4, add9, dim7, aug, m7b5, maj7#9, 9no5, trailing '*' mute markers,
     * simple parenthetical annotations, etc.) rather than an exhaustive
     * music-theory grammar. Anything that doesn't match — English words,
     * guitar-tab fragments, riff notation — is treated as "not a chord" and
     * the whole bracket is left untouched.
     */
    private static final Pattern QUALITY_PATTERN = Pattern.compile(
            "(?:maj|min|mmaj|mM|dim|aug|\\+|m|M)?"
                    + "[0-9]{0,2}"
                    + "(?:[#b-][0-9]{1,2})*"
                    + "(?:sus[0-9]{0,2})?"
                    + "(?:add[#b]?[0-9]{1,2})*"
                    + "(?:no[0-9]{1,2})?"
                    + "(?:\\([^()]*\\))?"
                    + "\\*?",
            Pattern.CASE_INSENSITIVE);

    /**
     * Transposes a ChordPro line by the specified number of half steps,
     * deriving flat-vs-sharp spelling from the target key.
     *
     * @param line The ChordPro formatted line
     * @param halfSteps Number of half steps to transpose (positive = up, negative = down)
     * @param targetKey The key being transposed into; determines accidental spelling
     * @return The transposed line
     */
    public String transpose(String line, int halfSteps, MusicalKey targetKey) {
        return transpose(line, halfSteps, targetKey.prefersFlats());
    }

    /**
     * Transposes a ChordPro line by the specified number of half steps.
     *
     * @param line The ChordPro formatted line
     * @param halfSteps Number of half steps to transpose (positive = up, negative = down)
     * @param useFlats Whether to prefer flat notation over sharp notation
     * @return The transposed line
     */
    public String transpose(String line, int halfSteps, boolean useFlats) {
        if (line == null || line.isEmpty()) {
            return line;
        }

        Matcher matcher = CHORD_PATTERN.matcher(line);
        StringBuilder result = new StringBuilder();
        int lastEnd = 0;

        while (matcher.find()) {
            result.append(line, lastEnd, matcher.start());

            String root = matcher.group(1);
            String quality = matcher.group(2);
            String bass = matcher.group(3);

            if (!isValidQuality(quality)) {
                // Not a real chord (section label, tab diagram, riff notation,
                // typo, ...) - leave the original bracket exactly as-is.
                result.append(matcher.group());
            } else {
                result.append('[').append(transposeNote(root, halfSteps, useFlats)).append(quality);
                if (bass != null) {
                    result.append('/').append(transposeNote(bass, halfSteps, useFlats));
                }
                result.append(']');
            }

            lastEnd = matcher.end();
        }
        result.append(line, lastEnd, line.length());

        return result.toString();
    }

    /**
     * Convenience method to transpose up by a number of half steps using sharp notation.
     */
    public String transposeUp(String line, int halfSteps) {
        return transpose(line, halfSteps, false);
    }

    /**
     * Convenience method to transpose down by a number of half steps using flat notation.
     */
    public String transposeDown(String line, int halfSteps) {
        return transpose(line, -halfSteps, true);
    }

    private boolean isValidQuality(String quality) {
        return quality != null && QUALITY_PATTERN.matcher(quality).matches();
    }

    /**
     * Transposes a single note (root or bass) by the specified number of
     * half steps. Returns the original text unchanged if it can't be parsed
     * as a note.
     */
    private String transposeNote(String note, int halfSteps, boolean useFlats) {
        int currentPosition = getNotePosition(note);
        if (currentPosition == -1) {
            return note;
        }

        int newPosition = Math.floorMod(currentPosition + halfSteps, 12);

        String[] scale = useFlats ? CHROMATIC_SCALE_FLATS : CHROMATIC_SCALE_SHARPS;
        return normalizeNote(scale[newPosition], useFlats);
    }

    /**
     * Gets the position of a note in the chromatic scale (0-11).
     * Handles notes with multiple accidentals.
     */
    private int getNotePosition(String note) {
        if (note == null || note.isEmpty()) {
            return -1;
        }

        // Normalize case
        note = note.substring(0, 1).toUpperCase() + note.substring(1).toLowerCase();

        char baseNote = note.charAt(0);
        int position;

        switch (baseNote) {
            case 'C': position = 0; break;
            case 'D': position = 2; break;
            case 'E': position = 4; break;
            case 'F': position = 5; break;
            case 'G': position = 7; break;
            case 'A': position = 9; break;
            case 'B': position = 11; break;
            default: return -1;
        }

        for (int i = 1; i < note.length(); i++) {
            char accidental = note.charAt(i);
            if (accidental == '#') {
                position++;
            } else if (accidental == 'b') {
                position--;
            }
        }

        return Math.floorMod(position, 12);
    }

    /**
     * Normalizes notes to handle double accidentals and enharmonic equivalents.
     */
    private String normalizeNote(String note, boolean useFlats) {
        if (ENHARMONIC_MAP.containsKey(note)) {
            return ENHARMONIC_MAP.get(note);
        }

        if (note.contains("##")) {
            String baseNote = note.substring(0, 1);
            int position = (getNotePosition(baseNote) + 2) % 12;
            String[] scale = useFlats ? CHROMATIC_SCALE_FLATS : CHROMATIC_SCALE_SHARPS;
            return scale[position];
        }

        if (note.contains("bb")) {
            String baseNote = note.substring(0, 1);
            int position = Math.floorMod(getNotePosition(baseNote) - 2, 12);
            String[] scale = useFlats ? CHROMATIC_SCALE_FLATS : CHROMATIC_SCALE_SHARPS;
            return scale[position];
        }

        if (note.contains("b#") || note.contains("#b")) {
            return note.substring(0, 1);
        }

        return note;
    }
}
