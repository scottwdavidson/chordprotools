package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import com.pourchoices.chordpro.application.port.in.CatalogProvider;
import com.pourchoices.chordpro.application.port.out.CatalogPort;
import com.pourchoices.chordpro.config.ChordproCatalogIndexPathConfig;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Load the song catalog from the disk.
 */
@Service
@AllArgsConstructor(onConstructor_ = @__(@Autowired))
@Slf4j
public class CatalogProviderService implements CatalogProvider {

    private final CatalogPort catalogPort;
    private final ChordproCatalogIndexPathConfig chordproCatalogIndexPathConfig;

    @Override
    public Map<String, CatalogEntry> loadCatalog(String chordproSongPathString) {

        // get the catalog path string and convert to a Path
        Path catalogPath = Paths.get(this.chordproCatalogIndexPathConfig.getCatalogIndexPath());

        // read the catalog
        Map<String, CatalogEntry> catalogMap = this.catalogPort.readCatalogFromCsv(catalogPath);

        // return it
        return catalogMap;
    }
}
