package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.adapter.out.file.CatalogEntryDto;
import com.pourchoices.chordpro.adapter.out.file.ChordProFileReader;
import com.pourchoices.chordpro.adapter.out.file.ChordProFileWriter;
import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import com.pourchoices.chordpro.application.domain.model.ParsedHeader;
import com.pourchoices.chordpro.application.domain.model.ParsedHeaderMapper;
import com.pourchoices.chordpro.application.domain.model.ParsedSong;
import com.pourchoices.chordpro.application.domain.port.in.UpdateSongUseCase;
import com.pourchoices.chordpro.application.domain.port.out.CatalogPort;
import com.pourchoices.chordpro.config.ChordproCatalogIndexPathConfig;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Service
public class UpdateSongService implements UpdateSongUseCase {

    private final CatalogPort catalogPort;
    private final ChordproCatalogIndexPathConfig chordproCatalogIndexPathConfig;
    private final ChordProFileReader chordproFileReader;
    private final SongParser songParser;
    private final ParsedHeaderMapper parsedHeaderMapper;
    private final ChordProFileWriter chordProFileWriter;

    public UpdateSongService(CatalogPort catalogPort,
                             ChordproCatalogIndexPathConfig chordproCatalogIndexPathConfig,
                             ChordProFileReader chordproFileReader,
                             SongParser songParser,
                             ParsedHeaderMapper parsedHeaderMapper,
                             ChordProFileWriter chordProFileWriter) {
        this.catalogPort = catalogPort;
        this.chordproCatalogIndexPathConfig = chordproCatalogIndexPathConfig;
        this.chordproFileReader = chordproFileReader;
        this.songParser = songParser;
        this.parsedHeaderMapper = parsedHeaderMapper;
        this.chordProFileWriter = chordProFileWriter;
    }

    @Override
    public void updateSong(String chordproSongPathString) {

        // get the catalog path string and convert to a Path
        Path catalogPath = Paths.get(this.chordproCatalogIndexPathConfig.getCatalogIndexPath());

        // read the catalog
        Map<String, CatalogEntry> catalogMap = this.catalogPort.readCatalogFromCsv(catalogPath);

        // find the song to be updated
        CatalogEntry catalogEntry = catalogMap.get(chordproSongPathString);

        // map the catalog entry into a parsed header
        ParsedHeader potentialReplacementParsedHeader = this.parsedHeaderMapper.fromCatalogEntry(catalogEntry);

        // parse the current song file
        Path chordproSongPath = Paths.get(chordproSongPathString);
        List<String> chordproFileAsList = this.chordproFileReader.read(chordproSongPath);
        ParsedSong currentParsedSong = this.songParser.parse(chordproSongPathString, chordproFileAsList);

        // replace the header if changed
        if (currentParsedSong.getParsedHeader().compareTo(potentialReplacementParsedHeader) != 0) {
            ParsedSong newSong = currentParsedSong.withHeader(potentialReplacementParsedHeader);

            // overwrite original with the new song
            this.chordProFileWriter.write(chordproSongPath, newSong);
        }
    }
}
