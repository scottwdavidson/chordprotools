package com.pourchoices.chordpro.application.port.in;

import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import java.util.Map;

public interface CatalogProvider {
    public Map<String, CatalogEntry> loadCatalog(String chordproSongPathString);

}
