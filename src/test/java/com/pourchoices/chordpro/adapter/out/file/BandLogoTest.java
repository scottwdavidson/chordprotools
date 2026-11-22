package com.pourchoices.chordpro.adapter.out.file;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** Unit tests for {@link BandLogo}. */
class BandLogoTest {

    @TempDir
    Path tempDir;

    @Test
    void aMissingFolder_meansNoLogo() {
        assertThat(BandLogo.dataUri(tempDir.resolve("nope"))).isEmpty();
    }

    @Test
    void aFolderWithoutAPng_meansNoLogo() throws Exception {
        Files.writeString(tempDir.resolve("notes.txt"), "not a logo");

        assertThat(BandLogo.dataUri(tempDir)).isEmpty();
    }

    @Test
    void encodesTheFirstPngAlphabetically_asADataUri() throws Exception {
        Files.write(tempDir.resolve("b.PNG"), new byte[]{9, 9});
        Files.write(tempDir.resolve("a.png"), new byte[]{1, 2, 3});

        assertThat(BandLogo.dataUri(tempDir)).contains("data:image/png;base64,AQID");
    }
}
