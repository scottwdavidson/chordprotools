package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import com.pourchoices.chordpro.application.domain.model.ChordProPath;
import com.pourchoices.chordpro.application.domain.service.CatalogEntryToParsedHeaderMapper;
import com.pourchoices.chordpro.application.domain.model.ParsedHeader;
import com.pourchoices.chordpro.application.domain.model.ParsedSong;
import com.pourchoices.chordpro.application.port.in.UpdateSongUseCase;
import com.pourchoices.chordpro.application.port.out.CatalogPort;
import com.pourchoices.chordpro.application.port.out.ChordProPort;
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

    private final CatalogPort catalogPort;
    private final ChordproCatalogIndexPathConfig chordproCatalogIndexPathConfig;
    private final ChordProPort chordProPort;
    private final SongParser songParser;
    private final CatalogEntryToParsedHeaderMapper catalogEntryToParsedHeaderMapper;

    @Override
    public void updateSong(String chordproSongPathString) {

        // load the catalog
        Path catalogPath = Paths.get(chordproCatalogIndexPathConfig.getCatalogIndexPath());
        Map<String, CatalogEntry> catalogMap = this.catalogPort.readCatalogFromCsv(catalogPath);

        // find the song to be updated (catalog keyed by song-ID string, not full file path)
        String songIdString = ChordProPath.toSongId(chordproSongPathString).toString();
        CatalogEntry catalogEntry = catalogMap.get(songIdString);

        if (catalogEntry == null) {
            log.error("Catalog Entry not found for song ID '{}' (derived from path: {})",
                    songIdString, chordproSongPathString);
            return;
        }

        // map the catalog entry into a parsed header
        ParsedHeader potentialCatalogReplacementParsedHeader =
                this.catalogEntryToParsedHeaderMapper.fromCatalogEntry(catalogEntry);

        // parse the current song file
        Path chordproSongPath = Paths.get(chordproSongPathString);
        List<String> chordproFileAsList = this.chordProPort.read(chordproSongPath);
        ParsedSong currentParsedSong = this.songParser.parse(chordproSongPathString, chordproFileAsList);

        // replace the header if changed
        if (currentParsedSong.getParsedHeader().compareTo(potentialCatalogReplacementParsedHeader) != 0) {
            ParsedSong newSong = currentParsedSong.withHeader(potentialCatalogReplacementParsedHeader);
            this.chordProPort.write(chordproSongPath, newSong);
        }
    }
}
