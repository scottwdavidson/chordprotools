package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import com.pourchoices.chordpro.application.domain.model.ImportResult;
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
 * <p>When {@code dryRun = true} the service computes and returns exactly
 * what would be appended without writing anything — the caller (CLI
 * adapter, or a future orchestrating service) decides how to present that
 * preview. This service never prints; see the hexagonal boundary notes in
 * {@code command-reference.md}.
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
    public ImportResult importNewSong(String chordproSongPathString, boolean dryRun) {

        // 1. Validate the file exists.
        Path songPath = Paths.get(chordproSongPathString);
        if (!Files.exists(songPath)) {
            throw new IllegalArgumentException(
                    "File not found: " + songPath.toAbsolutePath());
        }

        // 2. Parse the .cho file.
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

        // 3. Load catalog and guard against duplicates.
        Path catalogPath = Paths.get(catalogConfig.getCatalogIndexPath());
        Map<String, CatalogEntry> existing = catalogPort.readCatalogFromCsv(catalogPath);

        if (existing.containsKey(songIdStr)) {
            throw new IllegalArgumentException(
                    "SONG ID '" + songIdStr + "' already exists in song-catalog.csv. "
                    + "Use update-song to modify existing songs.");
        }

        // 4. Dry-run: return a preview, write nothing.
        if (dryRun) {
            return ImportResult.builder()
                    .catalogEntry(newEntry)
                    .dryRun(true)
                    .catalogSizeAfter(existing.size())
                    .build();
        }

        // 5. Append, sort, write.
        List<CatalogEntry> updated = new ArrayList<>(existing.values());
        updated.add(newEntry);
        updated.sort(Comparator.comparing(e -> e.getSongId().toString()));

        catalogPort.writeCatalogToCsv(catalogPath, updated);

        log.info("song-catalog.csv updated — {} entries total", updated.size());

        return ImportResult.builder()
                .catalogEntry(newEntry)
                .dryRun(false)
                .catalogSizeAfter(updated.size())
                .build();
    }
}
