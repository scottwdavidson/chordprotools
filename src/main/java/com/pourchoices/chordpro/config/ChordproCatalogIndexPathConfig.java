package com.pourchoices.chordpro.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
public class ChordproCatalogIndexPathConfig {

    @Value("${chordprotools.catalog-index}")
    private String chordproCatalogIndexPathString;

    public Path getChordproCatalogIndexPath() {
        return Path.of(chordproCatalogIndexPathString);
    }
}
