package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import com.pourchoices.chordpro.application.domain.model.ParsedSong;
import com.pourchoices.chordpro.application.port.in.ImportNewSongUseCase;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Registers a single new {@code .cho} file in {@code song-catalog.csv}.
 *
 * <p>The SONG ID is derived deterministically from the file path via
 * {@link com.pourchoices.chordpro.application.domain.model.ChordProPath#toSongId}
 * — the caller never constructs it manually.
 *
 * <h3>Guard-rails</h3>
 * <ul>
 *   <li>The {@code .cho} file must exist.</li>
 *   <li>The derived SONG ID must not already be present in the catalog.</li>
 * </ul>
 *
 * <p>When {@code dryRun = true} the service prints exactly what would be
 * appended and exits without writing anything — useful for verifying the
 * file path convention and checking how metadata parsed before committing.
 */
@Service
@AllArgsConstructor(onConstructor_ = @__(@Autowired))
@Slf4j
public class ImportNewSongService implements ImportNewSongUseCase {

    private final CatalogPort catalogPort;
    private final ChordProPort chordProPort;
    private final SongParser songParser;
    private final ParsedHeaderToCatalogEntryMapper parsedHeaderMapper;
    private final ChordproCatalogIndexPathConfig catalogConfig;

    @Override
    public void importNewSong(String chordproSongPathString, boolean dryRun) {

        // ── 1. Validate the file exists ──────────────────────────────────────
        Path songPath = Paths.get(chordproSongPathString);
        if (!Files.exists(songPath)) {
            throw new IllegalArgumentException(
                    "File not found: " + songPath.toAbsolutePath());
        }

        // ── 2. Parse the .cho file ───────────────────────────────────────────
        List<String> lines = chordProPort.read(songPath);
        ParsedSong parsedSong = songParser.parse(chordproSongPathString, lines);
        CatalogEntry newEntry = parsedHeaderMapper.toCatalogEntry(
                chordproSongPathString, parsedSong.getParsedHeader());

        if (newEntry == null) {
            throw new IllegalArgumentException(
                    "No recognisable directives found in: " + chordproSongPathString
                    + " — is this a valid ChordPro file?");
        }

        String songIdStr = newEntry.getSongId().toString();
        log.info("Derived SONG ID: {}", songIdStr);

        // ── 3. Load catalog and guard against duplicates ─────────────────────
        Path catalogPath = Paths.get(catalogConfig.getCatalogIndexPath());
        Map<String, CatalogEntry> existing = catalogPort.readCatalogFromCsv(catalogPath);

        if (existing.containsKey(songIdStr)) {
            throw new IllegalArgumentException(
                    "SONG ID '" + songIdStr + "' already exists in song-catalog.csv. "
                    + "Use update-song to modify existing songs.");
        }

        // ── 4. Dry-run: print and stop ───────────────────────────────────────
        if (dryRun) {
            System.out.println();
            System.out.println("DRY RUN — nothing written to song-catalog.csv");
            System.out.println();
            System.out.printf("  SONG ID   : %s%n", songIdStr);
            System.out.printf("  TITLE     : %s%n", newEntry.getTitle());
            System.out.printf("  ARTIST    : %s%n", newEntry.getArtist());
            System.out.printf("  KEY       : %s%n", newEntry.getKey());
            System.out.printf("  DURATION  : %s%n", newEntry.getDuration());
            System.out.printf("  TEMPO     : %s%n", nvl(newEntry.getTempo()));
            System.out.printf("  COUNTIN   : %s%n", nvl(newEntry.getCountin()));
            System.out.printf("  BACKING   : %s%n",
                    newEntry.getBackingType() != null ? newEntry.getBackingType().name() : "");
            System.out.printf("  RC SLOT   : %s%n", nvl(newEntry.getRcSlot()));
            System.out.printf("  NORD      : %s%n", nvl(newEntry.getNord()));
            System.out.printf("  ROLAND    : %s%n", nvl(newEntry.getRoland()));
            System.out.printf("  VE        : %s%n", nvl(newEntry.getVe()));
            System.out.printf("  PERF KEY  : %s%n", nvl(newEntry.getPerformanceKey()));
            System.out.printf("  SONG LABEL: %s%n", nvl(newEntry.getSongLabel()));
            System.out.println();
            return;
        }

        // ── 5. Append, sort, write ───────────────────────────────────────────
        List<CatalogEntry> updated = new ArrayList<>(existing.values());
        updated.add(newEntry);
        updated.sort(Comparator.comparing(e -> e.getSongId().toString()));

        catalogPort.writeCatalogToCsv(catalogPath, updated);

        System.out.printf("Imported '%s' (%s) as SONG ID: %s%n",
                newEntry.getTitle(), newEntry.getArtist(), songIdStr);
        log.info("song-catalog.csv updated — {} entries total", updated.size());
    }

    private static String nvl(String value) {
        return value != null ? value : "";
    }
}
