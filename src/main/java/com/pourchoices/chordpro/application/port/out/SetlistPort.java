package com.pourchoices.chordpro.application.port.out;

import com.pourchoices.chordpro.application.domain.model.CatalogEntry;

import java.nio.file.Path;
import java.util.List;

public interface SetlistPort {

    void writeSetlistToCsv(Path outputPath, List<CatalogEntry> setlistEntries);

}
