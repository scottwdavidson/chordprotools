package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.ChordProKeyReader;
import com.pourchoices.chordpro.application.domain.model.HeaderDirective;
import com.pourchoices.chordpro.application.domain.model.MusicalKey;
import com.pourchoices.chordpro.application.domain.model.ParsedHeader;
import com.pourchoices.chordpro.application.domain.model.ParsedHeaderLine;
import com.pourchoices.chordpro.application.domain.model.ParsedSong;
import com.pourchoices.chordpro.application.domain.model.TransposeResult;
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
    public TransposeResult transpose(String inputPathString, int offsetSemitones, String outputPathString) {

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

        String sourceKeyRaw = ChordProKeyReader.readRawKeyValue(parsedSong.getParsedHeader(), inputPathString);
        MusicalKey sourceKey = MusicalKey.parse(sourceKeyRaw);
        MusicalKey targetKey = sourceKey.transposeBy(offsetSemitones);
        String targetKeyName = targetKey.canonicalName();

        List<String> warnings = new ArrayList<>();
        ParsedHeader newHeader = withUpdatedKey(parsedSong.getParsedHeader(), targetKeyName);
        List<String> transposedLines = transposeBody(parsedSong.getLines(), offsetSemitones, targetKey, warnings);

        ParsedSong transposedSong = ParsedSong.builder()
                .parsedHeader(newHeader)
                .lines(transposedLines)
                .build();

        chordProPort.write(outputPath, transposedSong);

        log.info("transpose complete: {} ({} -> {}) -> {}",
                inputPathString, sourceKeyRaw, targetKeyName, outputPathString);

        return TransposeResult.builder()
                .inputPath(inputPathString)
                .outputPath(outputPathString)
                .sourceKeyRaw(sourceKeyRaw)
                .sourceKey(sourceKey)
                .targetKey(targetKey)
                .offsetSemitones(offsetSemitones)
                .warnings(warnings)
                .build();
    }

    private List<String> transposeBody(
            List<String> lines, int offsetSemitones, MusicalKey targetKey, List<String> warnings) {
        List<String> transposed = new ArrayList<>(lines.size());
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            collectUnrecognizedChordAttempts(line, i + 1, warnings);
            transposed.add(chordProTransposer.transpose(line, offsetSemitones, targetKey));
        }
        return transposed;
    }

    /**
     * Regression guardrail (see design doc §12): a bracket that starts with a
     * note letter but fails the chord-quality grammar is probably a typo or
     * bad copy-paste, not deliberate non-chord notation. Collected here
     * rather than printed directly — the adapter decides how to surface
     * warnings (see {@link TransposeResult#getWarnings()}), so this data
     * still gets caught going forward instead of lurking in the catalog.
     */
    private void collectUnrecognizedChordAttempts(String line, int bodyLineNumber, List<String> warnings) {
        for (String bracket : chordProTransposer.findUnrecognizedChordAttempts(line)) {
            warnings.add(String.format(
                    "body line %d looks like a chord but wasn't recognized (left untransposed): %s",
                    bodyLineNumber, bracket));
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
