package com.pourchoices.chordpro.adapter.out.file;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Unit tests for {@link FanSetlistSheetAdapter}. */
class FanSetlistSheetAdapterTest {

    @TempDir
    Path tempDir;

    private final FanSetlistSheetAdapter adapter = new FanSetlistSheetAdapter(new FanSetlistHtmlRenderer());

    @Test
    void createsTheOutputFolderWhenItDoesNotExist() {
        Path output = tempDir.resolve("work").resolve("fan-setlist.html");

        adapter.writeFanSetlistSheet(output, "2026-01-01-test", null, List.of());

        assertThat(output).exists();
    }

    @Test
    void overwritesAnExistingSheet() throws Exception {
        Path output = tempDir.resolve("fan-setlist.html");
        Files.writeString(output, "old content");

        adapter.writeFanSetlistSheet(output, "2026-01-01-test", "Somewhere", List.of());

        assertThat(Files.readString(output)).contains("<!DOCTYPE html>").doesNotContain("old content");
    }
}
