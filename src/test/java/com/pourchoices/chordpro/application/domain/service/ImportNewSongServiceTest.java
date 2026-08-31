package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import com.pourchoices.chordpro.application.domain.model.ImportResult;
import com.pourchoices.chordpro.application.domain.model.SongId;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ImportNewSongService}. Uses the real {@link SongParser}
 * / {@link ParsedHeaderToCatalogEntryMapper} collaborators (no external
 * dependencies) and mocks only the file-I/O and catalog ports, per the same
 * pattern as {@link TransposeServiceTest}.
 *
 * <p>{@link com.pourchoices.chordpro.application.domain.model.ChordProPath}
 * derives the SONG ID from a path that must literally start with
 * {@code "cho/"} (relative to the working directory), so — unlike most other
 * service tests — this one can't rely purely on {@code @TempDir} (which
 * always hands back an absolute path). Instead it creates a small,
 * clearly-fake fixture under the real {@code ./cho/} tree (cluster
 * {@code ZZZ}, which is not a real cluster prefix — see {@link SongId}) and
 * removes it in {@link #tearDown()}.
 */
class ImportNewSongServiceTest {

    private ChordProPort chordProPort;
    private CatalogPort catalogPort;
    private ImportNewSongService service;

    private static final String SONG_PATH_STRING = "cho/ZZZ/Z/ImportNewSongServiceTest/TempSong.cho";
    private Path songPath;

    private static final List<String> SIMPLE_SONG = List.of(
            "{title: Moving Out}",
            "{artist: Billy Joel}",
            "{key: C}",
            "{duration: 3:15}",
            "",
            "[C]Anthony works in the [F]grocery store"
    );

    @BeforeEach
    void setUp() throws IOException {
        chordProPort = mock(ChordProPort.class);
        catalogPort = mock(CatalogPort.class);
        ChordproCatalogIndexPathConfig config = mock(ChordproCatalogIndexPathConfig.class);
        when(config.getCatalogIndexPath()).thenReturn("./song-catalog.csv");

        service = new ImportNewSongService(
                catalogPort, chordProPort, new SongParser(new SongLineParser()),
                new ParsedHeaderToCatalogEntryMapper(), config);

        songPath = Paths.get(SONG_PATH_STRING);
        Files.createDirectories(songPath.getParent());
        Files.createFile(songPath);
    }

    @AfterEach
    void tearDown() throws IOException {
        // Clean up only the fixture directories this test created.
        Path artistDir = songPath.getParent();          // .../ZZZ/Z/ImportNewSongServiceTest
        Path elementDir = artistDir.getParent();         // .../ZZZ/Z
        Path clusterDir = elementDir.getParent();        // .../ZZZ
        Files.deleteIfExists(songPath);
        Files.deleteIfExists(artistDir);
        Files.deleteIfExists(elementDir);
        Files.deleteIfExists(clusterDir);
    }

    private void givenExistingCatalog(CatalogEntry... entries) {
        Map<String, CatalogEntry> catalog = new LinkedHashMap<>();
        for (CatalogEntry e : entries) {
            catalog.put(e.getSongId().toString(), e);
        }
        when(catalogPort.readCatalogFromCsv(any())).thenReturn(catalog);
    }

    @Test
    void realRun_appendsEntryAndWritesCatalog() {
        givenExistingCatalog();
        when(chordProPort.read(songPath)).thenReturn(SIMPLE_SONG);

        ImportResult result = service.importNewSong(SONG_PATH_STRING, false);

        assertThat(result.isDryRun()).isFalse();
        assertThat(result.getCatalogEntry().getTitle()).isEqualTo("Moving Out");
        assertThat(result.getCatalogSizeAfter()).isEqualTo(1);
        verify(catalogPort).writeCatalogToCsv(any(), any());
    }

    @Test
    void dryRun_doesNotWriteCatalog() {
        givenExistingCatalog();
        when(chordProPort.read(songPath)).thenReturn(SIMPLE_SONG);

        ImportResult result = service.importNewSong(SONG_PATH_STRING, true);

        assertThat(result.isDryRun()).isTrue();
        assertThat(result.getCatalogEntry().getTitle()).isEqualTo("Moving Out");
        assertThat(result.getCatalogSizeAfter()).isZero();
        verify(catalogPort, never()).writeCatalogToCsv(any(), any());
    }

    @Test
    void missingFile_throws() {
        String missing = "cho/ZZZ/Z/ImportNewSongServiceTest/DoesNotExist.cho";

        assertThatThrownBy(() -> service.importNewSong(missing, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File not found");
    }

    @Test
    void duplicateSongId_throws() {
        givenExistingCatalog(CatalogEntry.builder()
                .songId(SongId.parse("ZZZ:Z:ImportNewSongServiceTest:TempSong"))
                .title("Moving Out").artist("Billy Joel").key("C").duration("3:15")
                .build());
        when(chordProPort.read(songPath)).thenReturn(SIMPLE_SONG);

        assertThatThrownBy(() -> service.importNewSong(SONG_PATH_STRING, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists in song-catalog.csv");
    }

    @Test
    void noDirectives_throws() {
        when(chordProPort.read(songPath)).thenReturn(List.of("just a lyric line, no directives"));

        assertThatThrownBy(() -> service.importNewSong(SONG_PATH_STRING, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No recognisable directives found");
    }
}
