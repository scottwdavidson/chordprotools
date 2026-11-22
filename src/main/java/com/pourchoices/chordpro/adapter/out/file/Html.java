package com.pourchoices.chordpro.adapter.out.file;

import lombok.SneakyThrows;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Tiny HTML helpers shared by the printable-sheet renderers and adapters. */
final class Html {

    private Html() {
    }

    /** Escapes text for use as HTML element content or a quoted attribute; {@code null} becomes empty. */
    static String escape(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    /** Writes the document as UTF-8, creating the output folder if needed and overwriting any old file. */
    @SneakyThrows
    static void write(Path outputPath, String document) {
        Path folder = outputPath.toAbsolutePath().getParent();
        if (folder != null) Files.createDirectories(folder);
        Files.writeString(outputPath, document, StandardCharsets.UTF_8);
    }
}
