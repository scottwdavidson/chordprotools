package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.adapter.out.file.ChordProFileReader;
import com.pourchoices.chordpro.adapter.out.file.ChordProFileWriter;
import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import com.pourchoices.chordpro.application.domain.model.ParsedHeader;
import com.pourchoices.chordpro.application.domain.model.CatalogEntryToParsedHeaderMapper;
import com.pourchoices.chordpro.application.domain.model.ParsedSong;
import com.pourchoices.chordpro.application.port.in.CatalogProvider;
import com.pourchoices.chordpro.application.port.in.UpdateSongUseCase;
import com.pourchoices.chordpro.config.ChordproCatalogIndexPathConfig;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * Updates a chordpro song file based on the current song catalog metadata.
 */
@Service
@AllArgsConstructor(onConstructor_ = @__(@Autowired))
@Slf4j
public class UpdateSongService implements UpdateSongUseCase {

    private final CatalogProvider catalogProvider;
    private final ChordproCatalogIndexPathConfig chordproCatalogIndexPathConfig;
    private final ChordProFileReader chordproFileReader;
    private final SongParser songParser;
    private final CatalogEntryToParsedHeaderMapper catalogEntryToParsedHeaderMapper;
    private final ChordProFileWriter chordProFileWriter;

    @Override
    public void updateSong(String chordproSongPathString) {

        // load the catalog
        Map<String, CatalogEntry> catalogMap = this.catalogProvider.loadCatalog(
            this.chordproCatalogIndexPathConfig.getCatalogIndexPath());

        // find the song to be updated
        CatalogEntry catalogEntry = catalogMap.get(chordproSongPathString);

        if (catalogEntry == null) {
            log.error("Catalog Entry not found in Catalog Path: {}", chordproSongPathString);
            return;
        }

        // map the catalog entry into a parsed header
        ParsedHeader potentialCatalogReplacementParsedHeader = this.catalogEntryToParsedHeaderMapper.fromCatalogEntry(catalogEntry);

        // parse the current song file
        Path chordproSongPath = Paths.get(chordproSongPathString);
        List<String> chordproFileAsList = this.chordproFileReader.read(chordproSongPath);
        ParsedSong currentParsedSong = this.songParser.parse(chordproSongPathString, chordproFileAsList);

        // replace the header if changed
        if (currentParsedSong.getParsedHeader().compareTo(potentialCatalogReplacementParsedHeader) != 0) {
            ParsedSong newSong = currentParsedSong.withHeader(potentialCatalogReplacementParsedHeader);

            // overwrite original with the new song
            this.chordProFileWriter.write(chordproSongPath, newSong);
        }
    }
}
