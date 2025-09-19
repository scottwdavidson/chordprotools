package com.pourchoices.chordpro.application.domain.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashMap;
import java.util.Map;

/**
 * Transposes ChordPro format music lines from one key to another.
 * ChordPro format: [C]Something something, [Fmaj7], something something[Gm]
 */
public class ChordProTransposer {

    // Maps for note conversions
    private static final String[] CHROMATIC_SCALE = {
            "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"
    };

    private static final String[] CHROMATIC_SCALE_FLATS = {
            "C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B"
    };

    // Map to normalize enharmonic equivalents to standard notes
    private static final Map<String, String> ENHARMONIC_MAP = new HashMap<>();
    static {
        ENHARMONIC_MAP.put("E#", "F");
        ENHARMONIC_MAP.put("B#", "C");
        ENHARMONIC_MAP.put("Cb", "B");
        ENHARMONIC_MAP.put("Fb", "E");
    }

    // Pattern to match chord prefixes in square brackets
    private static final Pattern CHORD_PATTERN = Pattern.compile("\\[([A-G][#b]*)", Pattern.CASE_INSENSITIVE);

    /**
     * Transposes a ChordPro line by the specified number of half steps.
     *
     * @param line The ChordPro formatted line
     * @param halfSteps Number of half steps to transpose (positive = up, negative = down)
     * @param useFlats Whether to prefer flat notation over sharp notation
     * @return The transposed line
     */
    public static String transpose(String line, int halfSteps, boolean useFlats) {
        if (line == null || line.isEmpty()) {
            return line;
        }

        Matcher matcher = CHORD_PATTERN.matcher(line);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String originalChord = matcher.group(1);
            String transposedChord = transposeChord(originalChord, halfSteps, useFlats);
            matcher.appendReplacement(result, "[" + transposedChord);
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Transposes a single chord by the specified number of half steps.
     *
     * @param chord The chord to transpose (e.g., "C", "Bb", "F#")
     * @param halfSteps Number of half steps to transpose
     * @param useFlats Whether to prefer flat notation
     * @return The transposed chord
     */
    private static String transposeChord(String chord, int halfSteps, boolean useFlats) {
        // Extract the root note (letter + accidentals)
        String rootNote = extractRootNote(chord);

        // Get the current position in the chromatic scale
        int currentPosition = getNotePosition(rootNote);
        if (currentPosition == -1) {
            // If we can't parse the note, return the original chord
            return chord;
        }

        // Calculate new position
        int newPosition = (currentPosition + halfSteps) % 12;
        if (newPosition < 0) {
            newPosition += 12;
        }

        // Get the new root note
        String[] scale = useFlats ? CHROMATIC_SCALE_FLATS : CHROMATIC_SCALE;
        String newRootNote = scale[newPosition];

        // Handle enharmonic equivalents and double accidentals
        newRootNote = normalizeNote(newRootNote, useFlats);

        // Replace the root note in the original chord
        return chord.replaceFirst("^[A-G][#b]*", newRootNote);
    }

    /**
     * Extracts the root note from a chord (including accidentals).
     */
    private static String extractRootNote(String chord) {
        if (chord == null || chord.isEmpty()) {
            return chord;
        }

        // Match the root note pattern (letter + any number of sharps/flats)
        Pattern rootPattern = Pattern.compile("^([A-G][#b]*)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = rootPattern.matcher(chord);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return chord;
    }

    /**
     * Gets the position of a note in the chromatic scale (0-11).
     * Handles notes with multiple accidentals.
     */
    private static int getNotePosition(String note) {
        if (note == null || note.isEmpty()) {
            return -1;
        }

        // Normalize case
        note = note.substring(0, 1).toUpperCase() + note.substring(1).toLowerCase();

        // Start with the base note position
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

        // Apply accidentals
        for (int i = 1; i < note.length(); i++) {
            char accidental = note.charAt(i);
            if (accidental == '#') {
                position++;
            } else if (accidental == 'b') {
                position--;
            }
        }

        // Normalize to 0-11 range
        position = position % 12;
        if (position < 0) {
            position += 12;
        }

        return position;
    }

    /**
     * Normalizes notes to handle double accidentals and enharmonic equivalents.
     */
    private static String normalizeNote(String note, boolean useFlats) {
        // First, handle enharmonic equivalents
        if (ENHARMONIC_MAP.containsKey(note)) {
            return ENHARMONIC_MAP.get(note);
        }

        // Handle double accidentals and mixed accidentals
        if (note.contains("##")) {
            // Double sharp - move up two semitones from base note
            String baseNote = note.substring(0, 1);
            int position = getNotePosition(baseNote) + 2;
            position = position % 12;
            String[] scale = useFlats ? CHROMATIC_SCALE_FLATS : CHROMATIC_SCALE;
            return scale[position];
        }

        if (note.contains("bb")) {
            // Double flat - move down two semitones from base note
            String baseNote = note.substring(0, 1);
            int position = getNotePosition(baseNote) - 2;
            if (position < 0) position += 12;
            String[] scale = useFlats ? CHROMATIC_SCALE_FLATS : CHROMATIC_SCALE;
            return scale[position];
        }

        if (note.contains("b#") || note.contains("#b")) {
            // Mixed accidentals cancel out
            return note.substring(0, 1);
        }

        return note;
    }

    /**
     * Convenience method to transpose up by a number of half steps using sharp notation.
     */
    public static String transposeUp(String line, int halfSteps) {
        return transpose(line, halfSteps, false);
    }

    /**
     * Convenience method to transpose down by a number of half steps using flat notation.
     */
    public static String transposeDown(String line, int halfSteps) {
        return transpose(line, -halfSteps, true);
    }

    // Example usage and test method
    public static void main(String[] args) {
        // Test cases
        String testLine1 = "[C]Something something, [Fmaj7], something something[Gm]";
        String testLine2 = "[A]Test [Bb]chord [F#m7]progression";
        String testLine3 = "[E#]Weird [Cb]enharmonic [B#]equivalents";

        System.out.println("Original: " + testLine1);
        System.out.println("Up 2 half steps (sharps): " + transpose(testLine1, 2, false));
        System.out.println("Up 2 half steps (flats): " + transpose(testLine1, 2, true));
        System.out.println("Down 3 half steps (flats): " + transpose(testLine1, -3, true));
        System.out.println();

        System.out.println("Original: " + testLine2);
        System.out.println("Up 1 half step (sharps): " + transpose(testLine2, 1, false));
        System.out.println("Up 1 half step (flats): " + transpose(testLine2, 1, true));
        System.out.println();

        System.out.println("Original: " + testLine3);
        System.out.println("Up 0 half steps (normalized): " + transpose(testLine3, 0, false));
    }
}