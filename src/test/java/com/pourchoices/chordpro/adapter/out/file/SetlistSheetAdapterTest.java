package com.pourchoices.chordpro.adapter.out.file;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Unit tests for {@link SetlistSheetAdapter}. */
class SetlistSheetAdapterTest {

    @TempDir
    Path tempDir;

    private final SetlistSheetAdapter adapter = new SetlistSheetAdapter(new SetlistSheetHtmlRenderer());

    @Test
    void createsTheOutputFolderWhenItDoesNotExist() {
        Path output = tempDir.resolve("work").resolve("setlist.html");

        adapter.writeSetlistSheet(output, "2026-01-01-test", List.of());

        assertThat(output).exists();
    }

    @Test
    void overwritesAnExistingSheet() throws Exception {
        Path output = tempDir.resolve("setlist.html");
        Files.writeString(output, "old content");

        adapter.writeSetlistSheet(output, "2026-01-01-test", List.of());

        assertThat(Files.readString(output)).contains("<!DOCTYPE html>").doesNotContain("old content");
    }
}
