package com.pourchoices.chordpro.adapter.out.file;

import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Regression test for a real bug: {@link CatalogFileReader} used to collect
 * rows with {@code Collectors.toUnmodifiableMap}, which gives no ordering
 * guarantee. Any service that reads the catalog into that map and later
 * writes the whole thing back out (characterize-songs, import-song,
 * consistent-metadata --fix) was silently re-sorting the entire
 * song-catalog.csv into hash-bucket order on every write, destroying the
 * SONG-ID ordering the file is meant to keep (it mirrors the cho/ directory
 * tree). This test pins the fix: file order must survive a read.
 */
class CatalogFileReaderTest {

    private final CatalogEntryMapper mapper = new CatalogEntryMapper();
    private final CatalogFileReader reader = new CatalogFileReader(mapper);

    @Test
    void readCatalogFromCsv_preservesFileOrder_notHashOrder(@TempDir Path tempDir) throws Exception {
        Path src = Path.of("src/test/resources/catalog-order-test.csv");
        Path dest = tempDir.resolve("catalog-order-test.csv");
        Files.copy(src, dest);

        Map<String, CatalogEntry> catalog = reader.readCatalogFromCsv(dest);

        // Fixture is deliberately NOT alphabetical (Z, A, J) -- a hash-ordered
        // map would almost certainly not reproduce this exact sequence.
        assertThat(catalog.keySet()).containsExactly(
                "YZ:Z:ZeroArtist:ZeroToSixty",
                "ABC:A:AlphaArtist:AlphaSong",
                "JKL:J:MiddleArtist:MiddleSong");
    }

    @Test
    void readCatalogFromCsv_duplicateSongId_throwsRatherThanSilentlyDropping(@TempDir Path tempDir) throws Exception {
        Path dest = tempDir.resolve("duplicate-catalog.csv");
        Files.writeString(dest, String.join("\n", List.of(
                "TITLE,ARTIST,KEY,DURATION,SONG ID",
                "Song One,Artist,C,3:00,ABC:A:Artist:SongOne",
                "Song One Again,Artist,C,3:00,ABC:A:Artist:SongOne",
                "")));

        assertThatThrownBy(() -> reader.readCatalogFromCsv(dest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ABC:A:Artist:SongOne");
    }
}
