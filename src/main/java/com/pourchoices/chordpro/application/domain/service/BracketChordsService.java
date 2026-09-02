package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.BracketChordsReport;
import com.pourchoices.chordpro.application.domain.model.BracketChordsReport.Finding;
import com.pourchoices.chordpro.application.domain.model.BracketChordsReport.FindingType;
import com.pourchoices.chordpro.application.domain.model.BracketedLine;
import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import com.pourchoices.chordpro.application.domain.model.ChordProPath;
import com.pourchoices.chordpro.application.domain.model.ParsedSong;
import com.pourchoices.chordpro.application.port.in.BracketChordsUseCase;
import com.pourchoices.chordpro.application.port.out.CatalogPort;
import com.pourchoices.chordpro.application.port.out.ChordProPort;
import com.pourchoices.chordpro.config.ChordproCatalogIndexPathConfig;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Scans every song in the catalog for instrumental-notation drift. See
 * {@link BracketChordsUseCase} for the three finding types.
 *
 * <p>Only lines containing a {@code |} measure separator are examined -
 * this deliberately excludes ordinary lyric/chord-over-lyric lines, which
 * never use pipe-delimited measure notation in this catalog.
 */
@Service
@AllArgsConstructor(onConstructor_ = @__(@Autowired))
@Slf4j
public class BracketChordsService implements BracketChordsUseCase {

    private final CatalogPort catalogPort;
    private final ChordProPort chordProPort;
    private final SongParser songParser;
    private final ChordProTransposer chordProTransposer;
    private final ChordproCatalogIndexPathConfig catalogConfig;

    private static final Pattern REPEAT_SHORTHAND_PATTERN = Pattern.compile(
            ":\\|\\||\\bx\\s?[0-9]+\\b|\\brepeat\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern STRUM_SLASH_PATTERN = Pattern.compile("(^|\\s)/(\\s|$)");

    @Override
    public BracketChordsReport run(boolean fix) {
        Path catalogPath = Paths.get(catalogConfig.getCatalogIndexPath());
        Map<String, CatalogEntry> catalog = catalogPort.readCatalogFromCsv(catalogPath);

        List<Finding> findings = new ArrayList<>();
        int filesScanned = 0;
        int filesWithBareChords = 0;

        for (CatalogEntry entry : catalog.values()) {
            String filePath = ChordProPath.toFilePath(entry.getSongId());
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                continue; // verify-catalog's job to flag missing files
            }
            filesScanned++;

            List<String> lines = chordProPort.read(path);
            ParsedSong parsed = songParser.parse(filePath, lines);
            List<String> body = parsed.getLines();
            List<String> newLines = new ArrayList<>(body.size());
            boolean changed = false;

            for (int i = 0; i < body.size(); i++) {
                String line = body.get(i);
                int lineNumber = i + 1;

                if (!isInstrumentalShaped(line)) {
                    newLines.add(line);
                    continue;
                }

                if (REPEAT_SHORTHAND_PATTERN.matcher(line).find()) {
                    findings.add(finding(FindingType.REPEAT_SHORTHAND, entry.getSongId().toString(),
                            filePath, lineNumber, line, null));
                }
                if (STRUM_SLASH_PATTERN.matcher(line).find()) {
                    findings.add(finding(FindingType.STRUM_SLASH, entry.getSongId().toString(),
                            filePath, lineNumber, line, null));
                }

                BracketedLine bracketed = chordProTransposer.bracketBareChords(line);
                if (bracketed.changed()) {
                    findings.add(finding(FindingType.BARE_CHORD, entry.getSongId().toString(),
                            filePath, lineNumber, line, bracketed.getLine()));
                    changed = true;
                    newLines.add(fix ? bracketed.getLine() : line);
                } else {
                    newLines.add(line);
                }
            }

            if (changed) {
                if (fix) {
                    chordProPort.write(path, parsed.withLines(newLines));
                }
                filesWithBareChords++;
            }
        }

        log.info("bracket-chords: {} file(s) scanned, {} with bare chords, {} finding(s)",
                filesScanned, filesWithBareChords, findings.size());

        return BracketChordsReport.builder()
                .findings(findings)
                .filesScanned(filesScanned)
                .filesWithBareChords(filesWithBareChords)
                .build();
    }

    private boolean isInstrumentalShaped(String line) {
        return line != null && line.contains("|");
    }

    private Finding finding(FindingType type, String songId, String filePath,
                             int lineNumber, String originalLine, String fixedLine) {
        return Finding.builder()
                .type(type)
                .songId(songId)
                .filePath(filePath)
                .lineNumber(lineNumber)
                .originalLine(originalLine)
                .fixedLine(fixedLine)
                .build();
    }
}
