package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import com.pourchoices.chordpro.application.domain.model.ChordProKeyReader;
import com.pourchoices.chordpro.application.domain.model.ChordProPath;
import com.pourchoices.chordpro.application.domain.model.ImportResult;
import com.pourchoices.chordpro.application.domain.model.MusicalKey;
import com.pourchoices.chordpro.application.domain.model.ParsedSong;
import com.pourchoices.chordpro.application.domain.model.SongId;
import com.pourchoices.chordpro.application.domain.model.TransposeResult;
import com.pourchoices.chordpro.application.domain.model.TransposeSongResult;
import com.pourchoices.chordpro.application.port.in.TransposeSongUseCase;
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
 * Composes {@link TransposeService} and {@link ImportNewSongService} into
 * the "create a new key-variant, then catalog it" workflow — the same
 * service-injects-concrete-service composition {@link UpdateSongsService}
 * already uses for {@link UpdateSongService}. No business logic lives in the
 * shell script or the CLI adapter; this class is the one place that decides
 * how the two existing, independently-tested steps fit together.
 *
 * <h3>What this adds beyond just calling the two steps back-to-back</h3>
 * <ul>
 *   <li>Derives the target SONG ID/path automatically from the naming
 *       convention ({@link SongId#withKeyAlternative}) — the caller only
 *       supplies a source SONG ID and a semitone offset.</li>
 *   <li>Guards against creating a duplicate musical key within the same
 *       song group, even under a different filename (e.g. transposing a
 *       {@code -b} variant back down to the key the base file already
 *       covers) — not just a same-filename check.</li>
 *   <li>Guards against overwriting an existing target file.</li>
 *   <li>Never rolls back a successfully-written {@code .cho} file if the
 *       catalog step subsequently fails — see {@link TransposeSongResult}.</li>
 * </ul>
 */
@Service
@AllArgsConstructor(onConstructor_ = @__(@Autowired))
@Slf4j
public class TransposeSongService implements TransposeSongUseCase {

    private final TransposeService transposeService;
    private final ImportNewSongService importNewSongService;
    private final ChordProPort chordProPort;
    private final SongParser songParser;
    private final CatalogPort catalogPort;
    private final ChordproCatalogIndexPathConfig catalogConfig;

    @Override
    public TransposeSongResult transposeSong(String sourceSongIdString, int offsetSemitones, boolean skipImport) {

        SongId sourceSongId = SongId.parse(sourceSongIdString);
        String sourcePathString = ChordProPath.toFilePath(sourceSongId);
        Path sourcePath = Paths.get(sourcePathString);

        if (!Files.exists(sourcePath)) {
            throw new IllegalArgumentException(
                    "No .cho file found for SONG ID '" + sourceSongIdString + "' at " + sourcePathString
                    + " - use ./find-song-id to confirm the SONG ID.");
        }

        // Peek the source key ourselves (read-only) so the target SONG ID/path
        // can be computed and validated *before* anything gets written.
        List<String> lines = chordProPort.read(sourcePath);
        ParsedSong parsedSong = songParser.parse(sourcePathString, lines);
        MusicalKey sourceKey = ChordProKeyReader.readKey(parsedSong.getParsedHeader(), sourcePathString);
        MusicalKey targetKey = sourceKey.transposeBy(offsetSemitones);
        String targetKeySuffix = targetKey.canonicalName().toLowerCase();

        SongId targetSongId = sourceSongId.withKeyAlternative(targetKeySuffix);
        String targetPathString = ChordProPath.toFilePath(targetSongId);
        Path targetPath = Paths.get(targetPathString);

        guardAgainstExistingKeyInGroup(sourceSongId, targetKey);
        guardAgainstExistingFile(targetPath, targetPathString);

        // Step 1: transpose. Delegates entirely to the already-tested engine -
        // no transposition logic is duplicated here.
        TransposeResult transposeResult =
                transposeService.transpose(sourcePathString, offsetSemitones, targetPathString);

        if (skipImport) {
            log.info("transpose-song: {} -> {} (catalog import skipped)", sourceSongIdString, targetSongId);
            return TransposeSongResult.builder()
                    .sourceSongId(sourceSongId)
                    .targetSongId(targetSongId)
                    .transposeResult(transposeResult)
                    .importSkipped(true)
                    .build();
        }

        // Step 2: catalog import. Deliberately never rolled back on failure -
        // the .cho file we just wrote is valid content and stays put; the
        // caller decides how to report the partial outcome.
        try {
            ImportResult importResult = importNewSongService.importNewSong(targetPathString, false);
            log.info("transpose-song: {} -> {} (catalog import ok)", sourceSongIdString, targetSongId);
            return TransposeSongResult.builder()
                    .sourceSongId(sourceSongId)
                    .targetSongId(targetSongId)
                    .transposeResult(transposeResult)
                    .importResult(importResult)
                    .build();
        } catch (RuntimeException e) {
            log.warn("transpose-song: {} was written but catalog import failed: {}",
                    targetPathString, e.getMessage());
            return TransposeSongResult.builder()
                    .sourceSongId(sourceSongId)
                    .targetSongId(targetSongId)
                    .transposeResult(transposeResult)
                    .importFailureMessage(e.getMessage())
                    .build();
        }
    }

    /**
     * Fails if any existing catalog entry in this song's group already has
     * the target musical key (enharmonic-aware) — regardless of filename.
     * Catches, for example, transposing a {@code -b} variant back down to
     * the key the base file already covers.
     */
    private void guardAgainstExistingKeyInGroup(SongId sourceSongId, MusicalKey targetKey) {
        String groupKey = sourceSongId.toGroupKey();
        Map<String, CatalogEntry> catalog =
                catalogPort.readCatalogFromCsv(Paths.get(catalogConfig.getCatalogIndexPath()));

        for (CatalogEntry entry : catalog.values()) {
            if (!entry.getSongId().toGroupKey().equals(groupKey)) {
                continue;
            }
            if (!MusicalKey.isParseable(entry.getKey())) {
                continue; // don't let unrelated bad catalog data block a legitimate transpose
            }
            if (MusicalKey.parse(entry.getKey()).equals(targetKey)) {
                throw new IllegalArgumentException(
                        "SONG ID '" + entry.getSongId() + "' already exists in key "
                        + targetKey.canonicalName() + " - no need to transpose, it's already there.");
            }
        }
    }

    private void guardAgainstExistingFile(Path targetPath, String targetPathString) {
        if (Files.exists(targetPath)) {
            throw new IllegalArgumentException(
                    "Target file already exists: " + targetPathString
                    + " - refusing to overwrite. Pick a different offset, or remove it first.");
        }
    }
}
