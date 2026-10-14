package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import com.pourchoices.chordpro.application.domain.model.ChordProPath;
import com.pourchoices.chordpro.application.domain.model.ParsedSong;
import com.pourchoices.chordpro.application.domain.model.ParsedSongPhrase;
import com.pourchoices.chordpro.application.domain.model.SongDirective;
import com.pourchoices.chordpro.application.domain.model.TidySongPartsReport;
import com.pourchoices.chordpro.application.domain.model.TidySongPartsReport.Finding;
import com.pourchoices.chordpro.application.domain.model.TidySongPartsReport.FindingType;
import com.pourchoices.chordpro.application.domain.model.TidySongPartsReport.MatchConfidence;
import com.pourchoices.chordpro.application.port.in.TidySongPartsUseCase;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scans every song in the catalog for two ad-hoc section-marker shapes and
 * rewrites both as {@code {start_of_part}}/{@code {end_of_part}} pairs. See
 * {@link TidySongPartsUseCase} for the two shapes.
 */
@Service
@AllArgsConstructor(onConstructor_ = @__(@Autowired))
@Slf4j
public class TidySongPartsService implements TidySongPartsUseCase {

    private final CatalogPort catalogPort;
    private final ChordProPort chordProPort;
    private final SongParser songParser;
    private final SongLineParser songLineParser;
    private final ChordproCatalogIndexPathConfig catalogConfig;

    // Transformation A: {start_of_bridge[: Label]} / {end_of_bridge}
    private static final Pattern BRIDGE_START_PATTERN = Pattern.compile(
            "^\\{\\s*(?:start_of_bridge|sob)\\s*(?::\\s*([^}]*?)\\s*)?\\}$", Pattern.CASE_INSENSITIVE);
    private static final Pattern BRIDGE_END_PATTERN = Pattern.compile(
            "^\\{\\s*(?:end_of_bridge|eob)\\s*\\}$", Pattern.CASE_INSENSITIVE);
    private static final String DEFAULT_BRIDGE_LABEL = "Bridge";

    // Transformation B: {comment: ...Intro/Bridge/Solo/Instrumental/Outro/Pre-Chorus...}
    // Pre-Chorus is spelled three ways in the catalog: "Pre-Chorus", "PreChorus",
    // "Pre Chorus" - the optional [-\\s]? separator covers all three.
    private static final Pattern KEYWORD_PATTERN = Pattern.compile(
            "\\b(Intro|Bridge|Solo|Instrumental|Outro|Pre[-\\s]?Chorus)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern LEADING_KEYWORD_PATTERN = Pattern.compile(
            "^(Intro|Bridge|Solo|Instrumental|Outro|Pre[-\\s]?Chorus)\\b", Pattern.CASE_INSENSITIVE);

    @Override
    public TidySongPartsReport run(boolean fix) {
        Path catalogPath = Paths.get(catalogConfig.getCatalogIndexPath());
        Map<String, CatalogEntry> catalog = catalogPort.readCatalogFromCsv(catalogPath);

        List<Finding> findings = new ArrayList<>();
        int filesScanned = 0;
        int filesChanged = 0;

        for (CatalogEntry entry : catalog.values()) {
            String songId = entry.getSongId().toString();
            String filePath = ChordProPath.toFilePath(entry.getSongId());
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                continue; // verify-catalog's job to flag missing files
            }
            filesScanned++;

            List<String> lines = chordProPort.read(path);
            ParsedSong parsed = songParser.parse(filePath, lines);
            List<String> body = parsed.getLines();

            List<Finding> fileFindings = new ArrayList<>();
            List<String> afterBridgeFix = rewriteBridgeDirectives(body, songId, filePath, fileFindings);
            List<String> afterCommentFix = wrapCommentHeaders(afterBridgeFix, songId, filePath, fileFindings);

            boolean changed = !afterCommentFix.equals(body);
            if (changed) {
                findings.addAll(fileFindings);
                filesChanged++;
                if (fix) {
                    chordProPort.write(path, parsed.withLines(afterCommentFix));
                }
            }
        }

        log.info("tidy-song-parts: {} file(s) scanned, {} changed, {} finding(s)",
                filesScanned, filesChanged, findings.size());

        return TidySongPartsReport.builder()
                .findings(findings)
                .filesScanned(filesScanned)
                .filesChanged(filesChanged)
                .build();
    }

    // ── Transformation A: start_of_bridge / end_of_bridge ──────────────────

    private List<String> rewriteBridgeDirectives(List<String> body, String songId, String filePath,
                                                  List<Finding> findings) {
        List<String> result = new ArrayList<>(body.size());
        for (int i = 0; i < body.size(); i++) {
            String line = body.get(i);
            String trimmed = line.trim();

            Matcher startMatcher = BRIDGE_START_PATTERN.matcher(trimmed);
            Matcher endMatcher = BRIDGE_END_PATTERN.matcher(trimmed);

            if (startMatcher.matches()) {
                String label = startMatcher.group(1);
                if (label == null || label.isBlank()) {
                    label = DEFAULT_BRIDGE_LABEL;
                }
                result.add("{start_of_part: " + label + "}");
                findings.add(Finding.builder()
                        .type(FindingType.BRIDGE_DIRECTIVE)
                        .confidence(MatchConfidence.NONE)
                        .songId(songId)
                        .filePath(filePath)
                        .lineNumber(i + 1)
                        .label(label)
                        .contentLineCount(0)
                        .build());
            } else if (endMatcher.matches()) {
                result.add("{end_of_part}");
            } else {
                result.add(line);
            }
        }
        return result;
    }

    // ── Transformation B: {comment: Intro/Bridge/Solo/Instrumental/Outro} ──

    private List<String> wrapCommentHeaders(List<String> body, String songId, String filePath,
                                             List<Finding> findings) {
        List<String> result = new ArrayList<>(body.size());
        int i = 0;
        while (i < body.size()) {
            String line = body.get(i);
            String commentValue = asTargetComment(line);

            if (commentValue == null) {
                result.add(line);
                i++;
                continue;
            }

            MatchConfidence confidence = LEADING_KEYWORD_PATTERN.matcher(commentValue.trim()).find()
                    ? MatchConfidence.LEADING : MatchConfidence.EMBEDDED;

            result.add("{start_of_part: " + commentValue.trim() + "}");

            int j = i + 1;
            while (j < body.size() && !isBoundary(body.get(j))) {
                j++;
            }

            // content = body[i+1, j) with trailing blank lines pulled back out
            int lastContentIdx = j - 1;
            while (lastContentIdx >= i + 1 && body.get(lastContentIdx).isBlank()) {
                lastContentIdx--;
            }

            int contentLineCount = 0;
            for (int k = i + 1; k <= lastContentIdx; k++) {
                result.add(body.get(k));
                contentLineCount++;
            }
            result.add("{end_of_part}");
            for (int k = lastContentIdx + 1; k < j; k++) {
                result.add(body.get(k));
            }

            findings.add(Finding.builder()
                    .type(FindingType.COMMENT_HEADER)
                    .confidence(confidence)
                    .songId(songId)
                    .filePath(filePath)
                    .lineNumber(i + 1)
                    .label(commentValue.trim())
                    .contentLineCount(contentLineCount)
                    .build());

            i = j; // reprocess the boundary line itself on the next iteration
        }
        return result;
    }

    /**
     * @return the comment's text if {@code line} is a {@code {comment: ...}}
     *         whose text contains one of our target keywords as a whole
     *         word; {@code null} otherwise (including decorative
     *         {@code {comment:***}} dividers and unrelated asides).
     */
    private String asTargetComment(String line) {
        ParsedSongPhrase phrase = songLineParser.parseSongPhrase(line);
        if (phrase == null || phrase.getSongDirective() != SongDirective.SONG_COMMENT) {
            return null;
        }
        if (phrase.getLines().isEmpty()) {
            return null;
        }
        String value = phrase.getLines().get(0);
        if (value == null || !KEYWORD_PATTERN.matcher(value).find()) {
            return null;
        }
        return value;
    }

    /** True if {@code line} is a structural section-start directive (verse/chorus/bridge/part) or a target comment header. */
    private boolean isBoundary(String line) {
        if (asTargetComment(line) != null) {
            return true;
        }
        ParsedSongPhrase phrase = songLineParser.parseSongPhrase(line);
        if (phrase == null) {
            return false;
        }
        SongDirective directive = phrase.getSongDirective();
        return directive == SongDirective.VERSE
                || directive == SongDirective.CHORUS
                || directive == SongDirective.BRIDGE
                || directive == SongDirective.PART;
    }
}
