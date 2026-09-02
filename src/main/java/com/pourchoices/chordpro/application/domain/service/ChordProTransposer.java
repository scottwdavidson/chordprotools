package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.BracketedLine;
import com.pourchoices.chordpro.application.domain.model.MusicalKey;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    // Chromatic note names live on MusicalKey (single source of truth for
    // pitch-class spelling) - see MusicalKey.noteName().

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
     * Matches either an already-bracketed chord group (consumed and left
     * untouched) or a bare, unbracketed chord-shaped token: root note,
     * quality/extension text, optional slash bass. Mirrors
     * {@link #CHORD_PATTERN}'s inner grammar exactly but without requiring
     * surrounding brackets — used by {@link #bracketBareChords} to find
     * instrumental-notation chords typed without brackets (and therefore
     * invisible to {@link #transpose}).
     */
    private static final Pattern BARE_CHORD_PATTERN = Pattern.compile(
            "\\[[^\\]]*]"
                    + "|\\b([A-G][#b]*)([^/\\s|]*)(?:/([A-G][#b]*))?",
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
     * Known non-chord bracket annotations actually found in this catalog's
     * {@code .cho} files, grounded in a real grep audit rather than guessed:
     * section labels used as instrument/part cues. Extend this list
     * deliberately (not automatically) if a new one shows up - that's
     * exactly what {@link #findUnrecognizedChordAttempts} warning about it
     * first is for.
     */
    private static final Set<String> KNOWN_SECTION_LABELS = Set.of("bridge", "chorus", "bass");

    /**
     * Riff / scale-run notation: two or more space-separated note tokens,
     * e.g. {@code "E F# G A"}. Real lead-guitar riff annotations in this
     * catalog, not a chord.
     */
    private static final Pattern RIFF_PATTERN = Pattern.compile(
            "[A-G][#b]*(\\s+[A-G][#b]*)+", Pattern.CASE_INSENSITIVE);

    /**
     * A quality string ending in a parenthetical annotation preceded by
     * whitespace, e.g. {@code "m (9th - 1,3)"} or {@code " (17th - 3-6)"} -
     * real fret-position hints found in this catalog. Group 1 captures
     * whatever quality text (if any) precedes the hint.
     */
    private static final Pattern FRET_HINT_SUFFIX_PATTERN = Pattern.compile("^(.*?)\\s+\\([^()]*\\)$");

    /**
     * Distinguishes "legitimate non-chord bracket content we already know
     * about" from "probably a typo or bad copy-paste" for the
     * {@link #findUnrecognizedChordAttempts} warning guardrail. This is
     * deliberately a narrower, more conservative check than
     * {@link #isValidQuality} — {@code isValidQuality} answering "false" is
     * enough reason to leave a bracket untouched during transpose (safe
     * default), but it is NOT enough reason to warn about it, or every
     * {@code [Bridge]}/{@code [Chorus]} in the catalog would trigger a false
     * alarm every time.
     */
    private boolean isKnownNonChordAnnotation(String root, String quality) {
        String label = (root + quality).replaceAll(":$", "").trim().toLowerCase();
        if (KNOWN_SECTION_LABELS.contains(label)) {
            return true;
        }

        if (RIFF_PATTERN.matcher(root + quality).matches()) {
            return true;
        }

        Matcher fretHint = FRET_HINT_SUFFIX_PATTERN.matcher(quality);
        if (fretHint.matches()) {
            String precedingQuality = fretHint.group(1);
            return precedingQuality.isEmpty() || isValidQuality(precedingQuality);
        }

        return false;
    }

    /**
     * Scans a line for brackets that look like a chord attempt (start with a
     * note letter A-G) but fail the {@link #QUALITY_PATTERN} grammar AND
     * aren't a recognized non-chord annotation (section label, riff,
     * fret-position hint) - i.e. something a human probably meant as a
     * chord, but got typo'd, mangled by a bad copy-paste, or otherwise
     * doesn't parse. Used by the {@code transpose} command to warn instead
     * of silently leaving the bracket untouched, so bad chord data gets
     * caught the next time someone tries to transpose that song rather than
     * lurking forever.
     *
     * @return the exact unrecognized bracket text (e.g. {@code "[Fmjaj7]"}),
     *         empty if the whole line is clean
     */
    public List<String> findUnrecognizedChordAttempts(String line) {
        List<String> unrecognized = new ArrayList<>();
        if (line == null || line.isEmpty()) {
            return unrecognized;
        }

        Matcher matcher = CHORD_PATTERN.matcher(line);
        while (matcher.find()) {
            String root = matcher.group(1);
            String quality = matcher.group(2);
            if (!isValidQuality(quality) && !isKnownNonChordAnnotation(root, quality)) {
                unrecognized.add(matcher.group());
            }
        }
        return unrecognized;
    }

    /**
     * Wraps every bare (unbracketed) chord-shaped token in a line with
     * {@code [ ]} so it becomes visible to {@link #transpose}. Used by
     * {@code bracket-chords} to fix instrumental/dot-notation sections
     * written with bare chords (e.g. {@code | C . . . | G . . . |}) rather
     * than the bracketed convention (e.g. {@code | [C] . . . | [G] . . . |}).
     *
     * <p>Deliberately conservative, same philosophy as {@link #transpose}:
     * a token only gets wrapped if it fully parses as a chord via
     * {@link #isValidQuality} and isn't a
     * {@link #isKnownNonChordAnnotation known non-chord annotation}.
     * Already-bracketed groups are copied through untouched (never
     * double-wrapped). Anything else — dots, pipes, repeat shorthand,
     * stray lyric fragments, typos — is left exactly as-is.
     *
     * @return the (possibly unchanged) line plus the bare tokens that got wrapped
     */
    public BracketedLine bracketBareChords(String line) {
        if (line == null || line.isEmpty()) {
            return BracketedLine.builder().line(line == null ? "" : line).build();
        }

        Matcher matcher = BARE_CHORD_PATTERN.matcher(line);
        StringBuilder result = new StringBuilder();
        List<String> wrapped = new ArrayList<>();
        int lastEnd = 0;

        while (matcher.find()) {
            result.append(line, lastEnd, matcher.start());

            String root = matcher.group(1);
            if (root == null) {
                // Matched the already-bracketed alternative - leave as-is.
                result.append(matcher.group());
            } else {
                String quality = matcher.group(2);
                if (isValidQuality(quality) && !isKnownNonChordAnnotation(root, quality)) {
                    result.append('[').append(matcher.group()).append(']');
                    wrapped.add(matcher.group());
                } else {
                    result.append(matcher.group());
                }
            }

            lastEnd = matcher.end();
        }
        result.append(line, lastEnd, line.length());

        return BracketedLine.builder().line(result.toString()).wrappedTokens(wrapped).build();
    }

    /**
     * Extracts the root note of every recognized chord in a line, in order,
     * ignoring brackets that don't parse as a chord (section labels, riffs,
     * fret-hints, typos). Used by {@link SemanticDiffService} to compare
     * harmonic content between two files without duplicating the chord
     * grammar a second time.
     */
    public List<String> extractChordRoots(String line) {
        List<String> roots = new ArrayList<>();
        if (line == null || line.isEmpty()) {
            return roots;
        }

        Matcher matcher = CHORD_PATTERN.matcher(line);
        while (matcher.find()) {
            if (isValidQuality(matcher.group(2))) {
                roots.add(matcher.group(1));
            }
        }
        return roots;
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

        return normalizeNote(MusicalKey.noteName(newPosition, useFlats), useFlats);
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
            return MusicalKey.noteName(position, useFlats);
        }

        if (note.contains("bb")) {
            String baseNote = note.substring(0, 1);
            int position = Math.floorMod(getNotePosition(baseNote) - 2, 12);
            return MusicalKey.noteName(position, useFlats);
        }

        if (note.contains("b#") || note.contains("#b")) {
            return note.substring(0, 1);
        }

        return note;
    }
}
