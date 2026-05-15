package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import com.pourchoices.chordpro.application.domain.model.ChordProFileListing;
import com.pourchoices.chordpro.application.domain.model.ChordProPath;
import com.pourchoices.chordpro.application.domain.model.HeaderDirective;
import com.pourchoices.chordpro.application.domain.model.ParsedHeader;
import com.pourchoices.chordpro.application.domain.model.ParsedHeaderLine;
import com.pourchoices.chordpro.application.domain.model.ParsedSong;
import com.pourchoices.chordpro.application.port.in.GenerateSongCatalogUseCase;
import com.pourchoices.chordpro.application.port.out.CatalogPort;
import com.pourchoices.chordpro.application.port.out.ChordProPort;
import com.pourchoices.chordpro.config.ChordproCatalogIndexPathConfig;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor(onConstructor_ = @__(@Autowired))
@Slf4j
public class GenerateSongCatalogService implements GenerateSongCatalogUseCase {

    private final ReadSongListService readSongListService;
    private final CatalogPort catalogPort;
    private final ChordProPort chordProPort;
    private final SongParser songParser;
    private final ChordproCatalogIndexPathConfig chordproCatalogIndexPathConfig;

    public void generateSongCatalog(String songsListingPathString) {

        // read the song listing file
        ChordProFileListing chordProFileListing =
                this.readSongListService.readSongList(songsListingPathString);

        // parse each song and map its header metadata into a domain CatalogEntry
        List<CatalogEntry> catalogEntries = new ArrayList<>();
        for (String chordProFilename : chordProFileListing.getChordProFileNames()) {

            log.info("chordProFilename: {}", chordProFilename);

            List<String> songFile = this.chordProPort.read(Paths.get(chordProFilename));
            ParsedSong song = this.songParser.parse(chordProFilename, songFile);

            CatalogEntry entry = toCatalogEntry(chordProFilename, song.getParsedHeader());
            if (entry != null) {
                catalogEntries.add(entry);
            }
        }

        log.info("Catalog entries: {}", catalogEntries.size());

        Path catalogIndexPath = Paths.get(chordproCatalogIndexPathConfig.getCatalogIndexPath());
        log.info("catalogIndexPath: {}", catalogIndexPath);

        this.catalogPort.writeCatalogToCsv(catalogIndexPath, catalogEntries);
    }

    private CatalogEntry toCatalogEntry(String chordproFilename, ParsedHeader parsedHeader) {

        if (parsedHeader.getHeaderLines().isEmpty()) {
            return null;
        }

        CatalogEntry.CatalogEntryBuilder builder = CatalogEntry.builder()
                .songId(ChordProPath.toSongId(chordproFilename))
                .title("")
                .artist("")
                .key("")
                .duration("");

        for (ParsedHeaderLine line : parsedHeader.getHeaderLines()) {
            HeaderDirective d = line.getHeaderDirective();
            String v = line.getValue();

            if (d == HeaderDirective.TITLE)            builder.title(v);
            else if (d == HeaderDirective.ARTIST)      builder.artist(v);
            else if (d == HeaderDirective.KEY)         builder.key(v);
            else if (d == HeaderDirective.DURATION)    builder.duration(v);
            else if (d == HeaderDirective.TEMPO)       builder.tempo(v);
            else if (d == HeaderDirective.TIME_SIGNATURE) builder.timeSignature(v);
            else if (d == HeaderDirective.CAPO)        builder.capo(v);
            else if (d == HeaderDirective.NORD)        builder.nord(v);
            else if (d == HeaderDirective.ROLAND)      builder.roland(v);
            else if (d == HeaderDirective.COUNTIN)     builder.countin(v);
            else if (d == HeaderDirective.BACKING)     builder.backing(v);
            else if (d == HeaderDirective.SONG_LABEL)   builder.songLabel(v);
            else if (d == HeaderDirective.VE)          builder.ve(v);
            else if (d == HeaderDirective.PERFORMANCE_KEY) builder.performanceKey(v);
        }

        return builder.build();
    }
}
