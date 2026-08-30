package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.HeaderDirective;
import com.pourchoices.chordpro.application.domain.model.MusicalKey;
import com.pourchoices.chordpro.application.domain.model.ParsedHeader;
import com.pourchoices.chordpro.application.domain.model.ParsedHeaderLine;
import com.pourchoices.chordpro.application.domain.model.ParsedSong;
import com.pourchoices.chordpro.application.port.in.TransposeUseCase;
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

/**
 * Transposes a {@code .cho} file to a new key: rewrites the {@code {key:}}
 * directive and every chord in the body, spelling accidentals correctly for
 * the target key (see {@link ChordProTransposer} / {@link MusicalKey}).
 *
 * <p>Never overwrites the input file — {@code outputPath} is mandatory and
 * must differ from {@code inputPath}.
 */
@Service
@AllArgsConstructor(onConstructor_ = @__(@Autowired))
@Slf4j
public class TransposeService implements TransposeUseCase {

    private final ChordProPort chordProPort;
    private final SongParser songParser;
    private final ChordProTransposer chordProTransposer;

    @Override
    public void transpose(String inputPathString, int offsetSemitones, String outputPathString) {

        Path inputPath = Paths.get(inputPathString);
        if (!Files.exists(inputPath)) {
            throw new IllegalArgumentException("File not found: " + inputPath.toAbsolutePath());
        }

        Path outputPath = Paths.get(outputPathString);
        if (inputPath.toAbsolutePath().normalize().equals(outputPath.toAbsolutePath().normalize())) {
            throw new IllegalArgumentException(
                    "--output must not be the same file as the input: " + inputPathString);
        }

        List<String> lines = chordProPort.read(inputPath);
        ParsedSong parsedSong = songParser.parse(inputPathString, lines);

        ParsedHeaderLine keyLine = parsedSong.getParsedHeader().getHeaderLines().stream()
                .filter(l -> l.getHeaderDirective() == HeaderDirective.KEY)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No {key:} directive found in " + inputPathString
                                + " - can't transpose without knowing the source key."));

        MusicalKey sourceKey = MusicalKey.parse(keyLine.getValue());
        MusicalKey targetKey = sourceKey.transposeBy(offsetSemitones);
        String targetKeyName = targetKey.canonicalName();

        ParsedHeader newHeader = withUpdatedKey(parsedSong.getParsedHeader(), targetKeyName);
        List<String> transposedLines = transposeBody(parsedSong.getLines(), offsetSemitones, targetKey);

        ParsedSong transposedSong = ParsedSong.builder()
                .parsedHeader(newHeader)
                .lines(transposedLines)
                .build();

        chordProPort.write(outputPath, transposedSong);

        System.out.printf("Transposed %s: %s -> %s (%+d semitone%s) -> %s%n",
                inputPathString, keyLine.getValue(), targetKeyName,
                offsetSemitones, Math.abs(offsetSemitones) == 1 ? "" : "s", outputPathString);
        log.info("transpose complete: {} ({} -> {}) -> {}",
                inputPathString, keyLine.getValue(), targetKeyName, outputPathString);
    }

    private List<String> transposeBody(List<String> lines, int offsetSemitones, MusicalKey targetKey) {
        List<String> transposed = new ArrayList<>(lines.size());
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            warnOnUnrecognizedChordAttempts(line, i + 1);
            transposed.add(chordProTransposer.transpose(line, offsetSemitones, targetKey));
        }
        return transposed;
    }

    /**
     * Regression guardrail (see design doc §12): a bracket that starts with a
     * note letter but fails the chord-quality grammar is probably a typo or
     * bad copy-paste, not deliberate non-chord notation. Warn loudly rather
     * than silently leave it — this is how bad chord data gets caught going
     * forward instead of lurking in the catalog.
     */
    private void warnOnUnrecognizedChordAttempts(String line, int bodyLineNumber) {
        for (String bracket : chordProTransposer.findUnrecognizedChordAttempts(line)) {
            System.err.printf(
                    "WARNING: body line %d looks like a chord but wasn't recognized (left untransposed): %s%n",
                    bodyLineNumber, bracket);
        }
    }

    private ParsedHeader withUpdatedKey(ParsedHeader header, String targetKeyName) {
        ParsedHeader.ParsedHeaderBuilder builder = ParsedHeader.builder()
                .chordProFilename(header.getChordProFilename());
        for (ParsedHeaderLine line : header.getHeaderLines()) {
            if (line.getHeaderDirective() == HeaderDirective.KEY) {
                builder.headerLine(ParsedHeaderLine.builder()
                        .headerDirective(HeaderDirective.KEY)
                        .value(targetKeyName)
                        .build());
            } else {
                builder.headerLine(line);
            }
        }
        return builder.build();
    }
}
