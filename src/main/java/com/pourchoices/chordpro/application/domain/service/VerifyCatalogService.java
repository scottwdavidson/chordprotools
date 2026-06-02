package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import com.pourchoices.chordpro.application.domain.model.ChordProPath;
import com.pourchoices.chordpro.application.domain.model.ParsedSong;
import com.pourchoices.chordpro.application.port.in.VerifyCatalogUseCase;
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
import java.util.List;
import java.util.Map;

/**
 * Compares every entry in {@code song-catalog.csv} against its corresponding
 * {@code .cho} file and reports any discrepancies.
 *
 * <h3>Checks performed per entry</h3>
 * <ol>
 *   <li><b>MISSING FILE</b> — the expected {@code .cho} file does not exist on disk.</li>
 *   <li><b>DRIFT</b> — the file exists but one or more fields differ between
 *       the catalog and the file (i.e. catalog was edited but {@code update-song}
 *       has not been run, or the file was edited directly).</li>
 * </ol>
 *
 * <p>Prints a summary to stdout. Exits with a non-zero count so the shell
 * script can propagate a non-zero exit code for CI use.
 */
@Service
@AllArgsConstructor(onConstructor_ = @__(@Autowired))
@Slf4j
public class VerifyCatalogService implements VerifyCatalogUseCase {

    private final CatalogPort catalogPort;
    private final ChordProPort chordProPort;
    private final SongParser songParser;
    private final ParsedHeaderToCatalogEntryMapper parsedHeaderMapper;
    private final ChordproCatalogIndexPathConfig catalogConfig;
    private final CatalogEntryComparator comparator;

    @Override
    public int verifyCatalog() {

        Path catalogPath = Paths.get(catalogConfig.getCatalogIndexPath());
        Map<String, CatalogEntry> catalog = catalogPort.readCatalogFromCsv(catalogPath);

        log.info("Verifying {} catalog entries against .cho files...", catalog.size());

        int issues = 0;
        int clean  = 0;

        for (CatalogEntry catalogEntry : catalog.values()) {

            String filePath = ChordProPath.toFilePath(catalogEntry.getSongId());
            Path path = Paths.get(filePath);

            // ── Check 1: file must exist ─────────────────────────────────────
            if (!Files.exists(path)) {
                System.out.printf("[MISSING FILE] %s%n  Expected: %s%n",
                        catalogEntry.getSongId(), path.toAbsolutePath());
                issues++;
                continue;
            }

            // ── Check 2: parse file and compare field by field ───────────────
            List<String> lines = chordProPort.read(path);
            ParsedSong parsed = songParser.parse(filePath, lines);
            CatalogEntry fileEntry = parsedHeaderMapper.toCatalogEntry(filePath, parsed.getParsedHeader());

            List<String> diffs = comparator.diff(catalogEntry, fileEntry, comparator.allFields());
            if (diffs.isEmpty()) {
                clean++;
            } else {
                System.out.printf("[DRIFT] %s (%s)%n", catalogEntry.getSongId(), filePath);
                diffs.forEach(d -> System.out.println("  " + d));
                issues++;
            }
        }

        System.out.println();
        System.out.printf("verify-catalog: %d clean, %d issue(s) found%n", clean, issues);
        if (issues == 0) {
            System.out.println("All catalog entries match their .cho files. ✓");
        }

        return issues;
    }
}
