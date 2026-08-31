package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import com.pourchoices.chordpro.application.domain.model.ImportResult;
import com.pourchoices.chordpro.application.domain.model.MusicalKey;
import com.pourchoices.chordpro.application.domain.model.SongId;
import com.pourchoices.chordpro.application.domain.model.TransposeResult;
import com.pourchoices.chordpro.application.domain.model.TransposeSongResult;
import com.pourchoices.chordpro.application.port.out.CatalogPort;
import com.pourchoices.chordpro.application.port.out.ChordProPort;
import com.pourchoices.chordpro.config.ChordproCatalogIndexPathConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TransposeSongService} — the composed
 * "transpose, then catalog it" workflow. {@link TransposeService} and
 * {@link ImportNewSongService} are mocked here: both are already fully
 * covered by their own test classes, so these tests focus purely on
 * TransposeSongService's own job — deriving the target SONG ID/path,
 * the guard rails, and the composition/error-handling between the two
 * steps.
 *
 * <p>Uses a real fixture under {@code ./cho/} for the same reason
 * {@link ImportNewSongServiceTest} does — {@code ChordProPath} requires a
 * path starting with {@code cho/}, which {@code @TempDir} can't provide.
 * Cluster {@code YYY} / artist {@code TransposeSongServiceTest} is fake and
 * distinct from {@code ImportNewSongServiceTest}'s fixture.
 */
class TransposeSongServiceTest {

    private static final String CLUSTER = "YYY";
    private static final String ELEMENT = "Y";
    private static final String ARTIST = "TransposeSongServiceTest";
    private static final String TITLE = "TempSong";
    private static final String SOURCE_SONG_ID_STRING = CLUSTER + ":" + ELEMENT + ":" + ARTIST + ":" + TITLE;
    // Must match ChordProPath.toFilePath()'s "./cho/" prefix exactly - Path.equals()
    // does NOT treat "./cho/x" and "cho/x" as equal, even though both resolve to the
    // same file on disk. Mockito's argument matching relies on Path.equals().
    private static final String ARTIST_DIR_STRING = "./cho/" + CLUSTER + "/" + ELEMENT + "/" + ARTIST;
    private static final String SOURCE_PATH_STRING = ARTIST_DIR_STRING + "/" + TITLE + ".cho";

    private static final List<String> SOURCE_LINES = List.of(
            "{title: Temp Song}",
            "{artist: Transpose Song Service Test}",
            "{key: G}",
            "{duration: 3:00}",
            "",
            "[G]La [D]la [C]la"
    );

    private TransposeService transposeService;
    private ImportNewSongService importNewSongService;
    private ChordProPort chordProPort;
    private CatalogPort catalogPort;
    private TransposeSongService service;

    private Path sourcePath;
    private Path artistDir;

    @BeforeEach
    void setUp() throws IOException {
        transposeService = mock(TransposeService.class);
        importNewSongService = mock(ImportNewSongService.class);
        chordProPort = mock(ChordProPort.class);
        catalogPort = mock(CatalogPort.class);
        ChordproCatalogIndexPathConfig config = mock(ChordproCatalogIndexPathConfig.class);
        when(config.getCatalogIndexPath()).thenReturn("./song-catalog.csv");

        service = new TransposeSongService(
                transposeService, importNewSongService, chordProPort,
                new SongParser(new SongLineParser()), catalogPort, config);

        artistDir = Paths.get(ARTIST_DIR_STRING);
        sourcePath = Paths.get(SOURCE_PATH_STRING);
        Files.createDirectories(artistDir);
        Files.createFile(sourcePath);

        when(chordProPort.read(sourcePath)).thenReturn(SOURCE_LINES);
        givenCatalogGroup(); // empty by default
    }

    @AfterEach
    void tearDown() throws IOException {
        try (var files = Files.list(artistDir)) {
            for (Path f : files.toList()) {
                Files.deleteIfExists(f);
            }
        }
        Files.deleteIfExists(artistDir);
        Files.deleteIfExists(artistDir.getParent());          // .../YYY/Y
        Files.deleteIfExists(artistDir.getParent().getParent()); // .../YYY
    }

    private void givenCatalogGroup(CatalogEntry... entries) {
        Map<String, CatalogEntry> catalog = new LinkedHashMap<>();
        for (CatalogEntry e : entries) {
            catalog.put(e.getSongId().toString(), e);
        }
        when(catalogPort.readCatalogFromCsv(any())).thenReturn(catalog);
    }

    private static CatalogEntry entry(String songId, String key) {
        return CatalogEntry.builder()
                .songId(SongId.parse(songId))
                .title("Temp Song").artist("Transpose Song Service Test")
                .key(key).duration("3:00")
                .build();
    }

    private static TransposeResult fakeTransposeResult(String targetPath, MusicalKey targetKey, int offset) {
        return TransposeResult.builder()
                .inputPath(SOURCE_PATH_STRING)
                .outputPath(targetPath)
                .sourceKeyRaw("G")
                .sourceKey(MusicalKey.parse("G"))
                .targetKey(targetKey)
                .offsetSemitones(offset)
                .build();
    }

    // ── Happy path ───────────────────────────────────────────────────────

    @Test
    void transposeSong_derivesTargetPathAndComposesBothSteps() {
        String expectedTargetPath = ARTIST_DIR_STRING + "/" + TITLE + "-a.cho"; // G + 2 = A
        MusicalKey targetKey = MusicalKey.parse("A");
        when(transposeService.transpose(SOURCE_PATH_STRING, 2, expectedTargetPath))
                .thenReturn(fakeTransposeResult(expectedTargetPath, targetKey, 2));
        CatalogEntry importedEntry = entry(CLUSTER + ":" + ELEMENT + ":" + ARTIST + ":" + TITLE + "-a", "A");
        when(importNewSongService.importNewSong(eq(expectedTargetPath), eq(false)))
                .thenReturn(ImportResult.builder().catalogEntry(importedEntry).dryRun(false).catalogSizeAfter(1).build());

        TransposeSongResult result = service.transposeSong(SOURCE_SONG_ID_STRING, 2, false);

        assertThat(result.getTargetSongId().toString()).isEqualTo(CLUSTER + ":" + ELEMENT + ":" + ARTIST + ":" + TITLE + "-a");
        assertThat(result.isImportSuccessful()).isTrue();
        assertThat(result.isImportSkipped()).isFalse();
        verify(transposeService).transpose(SOURCE_PATH_STRING, 2, expectedTargetPath);
        verify(importNewSongService).importNewSong(expectedTargetPath, false);
    }

    @Test
    void transposeSong_fromExistingVariant_derivesSiblingInSameGroup() throws IOException {
        // Source is itself a key-variant (-g suffix) - needs its own fixture file,
        // since ChordProPath derives a different path for it than the base TITLE.
        String variantSongId = CLUSTER + ":" + ELEMENT + ":" + ARTIST + ":" + TITLE + "-g";
        String variantPathString = ARTIST_DIR_STRING + "/" + TITLE + "-g.cho";
        Path variantPath = Paths.get(variantPathString);
        Files.createFile(variantPath);
        when(chordProPort.read(variantPath)).thenReturn(SOURCE_LINES); // {key: G}, same as the base fixture

        String expectedTargetPath = ARTIST_DIR_STRING + "/" + TITLE + "-a.cho";
        when(transposeService.transpose(variantPathString, 2, expectedTargetPath))
                .thenReturn(fakeTransposeResult(expectedTargetPath, MusicalKey.parse("A"), 2));
        when(importNewSongService.importNewSong(any(), anyBoolean()))
                .thenReturn(ImportResult.builder()
                        .catalogEntry(entry(CLUSTER + ":" + ELEMENT + ":" + ARTIST + ":" + TITLE + "-a", "A"))
                        .dryRun(false).catalogSizeAfter(1).build());

        TransposeSongResult result = service.transposeSong(variantSongId, 2, false);

        assertThat(result.getTargetSongId().toGroupKey())
                .isEqualTo(CLUSTER + ":" + ELEMENT + ":" + ARTIST + ":" + TITLE);
        assertThat(result.getTargetSongId().toString())
                .isEqualTo(CLUSTER + ":" + ELEMENT + ":" + ARTIST + ":" + TITLE + "-a");
    }

    // ── --no-import ──────────────────────────────────────────────────────

    @Test
    void transposeSong_skipImport_neverCallsImportStep() {
        String expectedTargetPath = ARTIST_DIR_STRING + "/" + TITLE + "-a.cho";
        when(transposeService.transpose(SOURCE_PATH_STRING, 2, expectedTargetPath))
                .thenReturn(fakeTransposeResult(expectedTargetPath, MusicalKey.parse("A"), 2));

        TransposeSongResult result = service.transposeSong(SOURCE_SONG_ID_STRING, 2, true);

        assertThat(result.isImportSkipped()).isTrue();
        assertThat(result.isImportSuccessful()).isFalse();
        assertThat(result.isImportFailed()).isFalse();
        verify(importNewSongService, never()).importNewSong(any(), anyBoolean());
    }

    // ── Import-failure handling (no rollback) ───────────────────────────

    @Test
    void transposeSong_importFails_reportsFailureButKeepsTransposeResult() {
        String expectedTargetPath = ARTIST_DIR_STRING + "/" + TITLE + "-a.cho";
        when(transposeService.transpose(SOURCE_PATH_STRING, 2, expectedTargetPath))
                .thenReturn(fakeTransposeResult(expectedTargetPath, MusicalKey.parse("A"), 2));
        when(importNewSongService.importNewSong(eq(expectedTargetPath), eq(false)))
                .thenThrow(new IllegalArgumentException("song-catalog.csv is locked"));

        TransposeSongResult result = service.transposeSong(SOURCE_SONG_ID_STRING, 2, false);

        assertThat(result.isImportFailed()).isTrue();
        assertThat(result.isImportSuccessful()).isFalse();
        assertThat(result.getImportFailureMessage()).isEqualTo("song-catalog.csv is locked");
        // The transpose step is reported as having completed - no rollback attempted.
        assertThat(result.getTransposeResult().getOutputPath()).isEqualTo(expectedTargetPath);
        verify(transposeService).transpose(SOURCE_PATH_STRING, 2, expectedTargetPath);
    }

    // ── Guard rails ──────────────────────────────────────────────────────

    @Test
    void transposeSong_missingSourceFile_throws() {
        String missingSongId = CLUSTER + ":" + ELEMENT + ":" + ARTIST + ":DoesNotExist";

        assertThatThrownBy(() -> service.transposeSong(missingSongId, 2, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No .cho file found");
    }

    @Test
    void transposeSong_targetFileAlreadyExists_throws() throws IOException {
        Path existingTarget = Paths.get(ARTIST_DIR_STRING + "/" + TITLE + "-a.cho");
        Files.createFile(existingTarget);

        assertThatThrownBy(() -> service.transposeSong(SOURCE_SONG_ID_STRING, 2, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Target file already exists");
    }

    @Test
    void transposeSong_targetKeyAlreadyExistsInGroup_sameSpelling_throws() {
        givenCatalogGroup(entry(CLUSTER + ":" + ELEMENT + ":" + ARTIST + ":" + TITLE + "-a", "A"));

        assertThatThrownBy(() -> service.transposeSong(SOURCE_SONG_ID_STRING, 2, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(CLUSTER + ":" + ELEMENT + ":" + ARTIST + ":" + TITLE + "-a")
                .hasMessageContaining("already exists in key");
    }

    /**
     * Proves the guard compares musical keys, not filename suffixes or raw
     * strings: G transposed +2 lands on a key that {@link MusicalKey} spells
     * as "A" (no enharmonic ambiguity at that position), so this test picks
     * a source/offset that lands on a genuinely ambiguous position (C#/Db)
     * to prove the existing catalog's sharp spelling still blocks a
     * would-be flat-spelled duplicate.
     */
    @Test
    void transposeSong_targetKeyAlreadyExistsInGroup_enharmonicSpelling_throws() throws IOException {
        // Source key B, +2 semitones = C#/Db (an ambiguous position - MusicalKey spells it "Db").
        when(chordProPort.read(sourcePath)).thenReturn(List.of(
                "{title: Temp Song}", "{artist: Transpose Song Service Test}",
                "{key: B}", "{duration: 3:00}", "", "[B]La"
        ));
        givenCatalogGroup(entry(CLUSTER + ":" + ELEMENT + ":" + ARTIST + ":" + TITLE + "-c#", "C#"));

        assertThatThrownBy(() -> service.transposeSong(SOURCE_SONG_ID_STRING, 2, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(CLUSTER + ":" + ELEMENT + ":" + ARTIST + ":" + TITLE + "-c#");
    }

    @Test
    void transposeSong_unparseableCatalogKey_doesNotBlockLegitimateTranspose() {
        // Bad/legacy catalog data (e.g. a blank KEY) shouldn't crash the guard or block a real transpose.
        givenCatalogGroup(entry(CLUSTER + ":" + ELEMENT + ":" + ARTIST + ":" + TITLE + "-weird", ""));
        String expectedTargetPath = ARTIST_DIR_STRING + "/" + TITLE + "-a.cho";
        when(transposeService.transpose(SOURCE_PATH_STRING, 2, expectedTargetPath))
                .thenReturn(fakeTransposeResult(expectedTargetPath, MusicalKey.parse("A"), 2));

        TransposeSongResult result = service.transposeSong(SOURCE_SONG_ID_STRING, 2, true);

        assertThat(result.getTargetSongId().toString()).endsWith("-a");
    }
}
