package com.pourchoices.chordpro.adapter.out.file;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Finds the band's logo: the first {@code .png} (alphabetically) in the logo folder, as a
 * {@code data:} URI so the printed HTML stays one self-contained file. No folder or no PNG
 * simply means no logo - the handout falls back to a text heading.
 */
@Slf4j
final class BandLogo {

    private BandLogo() {
    }

    static Optional<String> dataUri(Path logoFolder) {
        if (!Files.isDirectory(logoFolder)) return Optional.empty();
        try (Stream<Path> files = Files.list(logoFolder)) {
            return files.filter(file -> file.getFileName().toString().toLowerCase().endsWith(".png"))
                    .sorted()
                    .findFirst()
                    .map(BandLogo::encode);
        } catch (IOException e) {
            log.warn("Could not read logo folder {}: {}", logoFolder, e.getMessage());
            return Optional.empty();
        }
    }

    private static String encode(Path png) {
        try {
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(Files.readAllBytes(png));
        } catch (IOException e) {
            throw new IllegalStateException("Could not read logo " + png + ": " + e.getMessage(), e);
        }
    }
}
