package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.HeaderDirective;
import com.pourchoices.chordpro.application.domain.model.MusicalKey;
import com.pourchoices.chordpro.application.domain.model.ParsedSong;
import com.pourchoices.chordpro.application.domain.model.SemanticDiffReport;
import com.pourchoices.chordpro.application.domain.model.SemanticDiffReport.Finding;
import com.pourchoices.chordpro.application.domain.model.SemanticDiffReport.FindingType;
import com.pourchoices.chordpro.application.port.in.VerifySyncUseCase;
import com.pourchoices.chordpro.application.port.out.ChordProPort;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Generic, catalog-agnostic drift engine behind {@code verify-sync}. Compares
 * two {@code .cho} files for semantic drift that survives stripping out pure
 * transposition and enharmonic respelling — see design doc §7.
 *
 * <p>Two independent checks, both by body line position:
 * <ul>
 *   <li><b>Lyric drift</b> — strip chords/directives, normalize whitespace,
 *       compare the remaining text line-by-line.</li>
 *   <li><b>Harmonic drift</b> — convert each chord's root to a Roman-numeral
 *       scale degree relative to <em>that file's own</em> {@code {key:}}, and
 *       compare the resulting degree sequences. A pure transposition (same
 *       song, different key) always produces the same degree sequence, so it
 *       never false-positives.</li>
 * </ul>
 *
 * <p><b>Known v1 simplification</b> (documented in the design doc, not
 * silently swept under the rug): the harmonic check compares scale degree
 * only, not the full chord extension. Two chords with the same root and
 * different quality/extension on the same degree won't be flagged here —
 * that's a real, deliberate limitation, not an oversight.
 */
@Service
@AllArgsConstructor(onConstructor_ = @__(@Autowired))
@Slf4j
public class SemanticDiffService implements VerifySyncUseCase {

    private final ChordProPort chordProPort;
    private final SongParser songParser;
    private final ChordProTransposer chordProTransposer;

    @Override
    public SemanticDiffReport verifySync(String fileAPath, String fileBPath) {
        ParsedSong songA = readAndParse(fileAPath);
        ParsedSong songB = readAndParse(fileBPath);

        List<Finding> findings = new ArrayList<>();
        findings.addAll(checkLyricDrift(songA.getLines(), songB.getLines()));
        findings.addAll(checkHarmonicDrift(songA, songB, fileAPath, fileBPath));

        return SemanticDiffReport.builder()
                .fileA(fileAPath)
                .fileB(fileBPath)
                .findings(findings)
                .build();
    }

    private ParsedSong readAndParse(String pathString) {
        Path path = Paths.get(pathString);
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("File not found: " + path.toAbsolutePath());
        }
        return songParser.parse(pathString, chordProPort.read(path));
    }

    // ── Lyric drift ──────────────────────────────────────────────────────────

    private List<Finding> checkLyricDrift(List<String> linesA, List<String> linesB) {
        List<Finding> findings = new ArrayList<>();
        int maxLines = Math.max(linesA.size(), linesB.size());

        for (int i = 0; i < maxLines; i++) {
            String strippedA = i < linesA.size() ? stripAnnotations(linesA.get(i)) : null;
            String strippedB = i < linesB.size() ? stripAnnotations(linesB.get(i)) : null;

            if (!Objects.equals(strippedA, strippedB)) {
                findings.add(Finding.builder()
                        .type(FindingType.LYRIC_DESYNC)
                        .lineNumber(i + 1)
                        .detail(String.format("File A: \"%s\"  |  File B: \"%s\"",
                                nvl(strippedA), nvl(strippedB)))
                        .build());
            }
        }
        return findings;
    }

    private static String stripAnnotations(String line) {
        if (line == null) {
            return "";
        }
        return line.replaceAll("\\[[^]]*]|\\{[^}]*}", "").trim().replaceAll("\\s+", " ");
    }

    // ── Harmonic drift ───────────────────────────────────────────────────────

    private List<Finding> checkHarmonicDrift(ParsedSong songA, ParsedSong songB,
                                              String fileAPath, String fileBPath) {
        MusicalKey keyA = extractKey(songA, fileAPath);
        MusicalKey keyB = extractKey(songB, fileBPath);

        List<String> linesA = songA.getLines();
        List<String> linesB = songB.getLines();
        int maxLines = Math.max(linesA.size(), linesB.size());

        List<Finding> findings = new ArrayList<>();
        for (int i = 0; i < maxLines; i++) {
            List<String> rootsA = i < linesA.size()
                    ? chordProTransposer.extractChordRoots(linesA.get(i)) : List.of();
            List<String> rootsB = i < linesB.size()
                    ? chordProTransposer.extractChordRoots(linesB.get(i)) : List.of();

            int maxChords = Math.max(rootsA.size(), rootsB.size());
            for (int c = 0; c < maxChords; c++) {
                String rootA = c < rootsA.size() ? rootsA.get(c) : null;
                String rootB = c < rootsB.size() ? rootsB.get(c) : null;

                String degreeA = rootA != null ? romanNumeral(rootA, keyA) : null;
                String degreeB = rootB != null ? romanNumeral(rootB, keyB) : null;

                if (!Objects.equals(degreeA, degreeB)) {
                    findings.add(Finding.builder()
                            .type(FindingType.HARMONIC_DRIFT)
                            .lineNumber(i + 1)
                            .detail(String.format("File A has %s (%s), File B has %s (%s)",
                                    nvl(degreeA), nvl(rootA), nvl(degreeB), nvl(rootB)))
                            .build());
                }
            }
        }
        return findings;
    }

    private static String romanNumeral(String rootNote, MusicalKey fileKey) {
        int rootPosition = MusicalKey.parse(rootNote).getChromaticPosition();
        return fileKey.romanNumeralDegree(rootPosition);
    }

    private static MusicalKey extractKey(ParsedSong song, String pathString) {
        return song.getParsedHeader().getHeaderLines().stream()
                .filter(l -> l.getHeaderDirective() == HeaderDirective.KEY)
                .findFirst()
                .map(l -> MusicalKey.parse(l.getValue()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "No {key:} directive found in " + pathString
                                + " - can't compute harmonic drift without a key."));
    }

    private static String nvl(String value) {
        return value != null ? value : "\u2014"; // em dash for "missing"
    }
}
