package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.adapter.out.file.CatalogEntryDto;
import com.pourchoices.chordpro.adapter.out.file.CatalogFileWriter;
import com.pourchoices.chordpro.adapter.out.file.ChordProFileReader;
import com.pourchoices.chordpro.adapter.out.file.SongListingFileReader;
import com.pourchoices.chordpro.application.domain.model.HeaderDirective;
import com.pourchoices.chordpro.application.domain.model.ParsedHeader;
import com.pourchoices.chordpro.application.domain.model.ParsedHeaderLine;
import com.pourchoices.chordpro.application.domain.model.ParsedSong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
public class GenerateIndexService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateIndexService.class);

    private final CatalogFileWriter catalogFileWriter;
    private final SongListingFileReader songListingFileReader;

    private final ChordProFileReader chordProFileReader;
    private final SongParser songParser;


    public GenerateIndexService(CatalogFileWriter catalogFileWriter,
                                SongListingFileReader songListingFileReader,
                                ChordProFileReader chordProFileReader,
                                SongParser songParser) {

        this.catalogFileWriter = catalogFileWriter;
        this.songListingFileReader = songListingFileReader;
        this.chordProFileReader = chordProFileReader;
        this.songParser = songParser;
    }
    public void generateIndex(String songsListingPathString, String indexPathString){

        // read the song catalog path string file
        List<String> songsListing = this.songListingFileReader.read(songsListingPathString);

        // parse each song and insert the metadata into the index
        List<CatalogEntryDto> catalogEntryDtos = new ArrayList<>();
        for(String songListing: songsListing) {

            LOGGER.info("songListing: {}", songListing);

            // TODO add songListing to parse
            List<String> songFile = this.chordProFileReader.read(songListing);
            ParsedSong song = this.songParser.parse(songFile);

            ParsedHeader parsedHeader = song.getParsedHeader();
            catalogEntryDtos.add(toCatalogEntryDto(parsedHeader));

        }

        LOGGER.info("Catalog DTOs: {}", catalogEntryDtos);

        Path indexPath = Path.of(indexPathString);
        this.catalogFileWriter.writeCatalogToCsv(indexPath,catalogEntryDtos);
    }

    private CatalogEntryDto toCatalogEntryDto(ParsedHeader parsedHeader){

        CatalogEntryDto.CatalogEntryDtoBuilder builder = CatalogEntryDto.builder();

        if ( parsedHeader.getHeaderLines().isEmpty() ) {
            return null;
        }

        for (ParsedHeaderLine parsedHeaderLine : parsedHeader.getHeaderLines()) {

            if (parsedHeaderLine.getHeaderDirective() == HeaderDirective.TITLE){
                builder.title(parsedHeaderLine.getValue());
            }
            else if (parsedHeaderLine.getHeaderDirective() == HeaderDirective.ARTIST){
                builder.artist(parsedHeaderLine.getValue());
            }
            else if (parsedHeaderLine.getHeaderDirective() == HeaderDirective.KEY){
                builder.key(parsedHeaderLine.getValue());
            }
            else if (parsedHeaderLine.getHeaderDirective() == HeaderDirective.DURATION){
                builder.duration(parsedHeaderLine.getValue());
            }
            else if (parsedHeaderLine.getHeaderDirective() == HeaderDirective.TEMPO){
                builder.tempo(parsedHeaderLine.getValue());
            }
            else if (parsedHeaderLine.getHeaderDirective() == HeaderDirective.TIME_SIGNATURE){
                builder.timeSignature(parsedHeaderLine.getValue());
            }
            else if (parsedHeaderLine.getHeaderDirective() == HeaderDirective.CAPO){
                builder.capo(parsedHeaderLine.getValue());
            }
            else if (parsedHeaderLine.getHeaderDirective() == HeaderDirective.NORD){
                builder.nord(parsedHeaderLine.getValue());
            }
            else if (parsedHeaderLine.getHeaderDirective() == HeaderDirective.VERSION){
                builder.version(parsedHeaderLine.getValue());
            }
            else if (parsedHeaderLine.getHeaderDirective() == HeaderDirective.COUNTIN){
                builder.countin(parsedHeaderLine.getValue());
            }
            else if (parsedHeaderLine.getHeaderDirective() == HeaderDirective.BACKING){
                builder.backing(parsedHeaderLine.getValue());
            }
            else if (parsedHeaderLine.getHeaderDirective() == HeaderDirective.VE){
                builder.ve(parsedHeaderLine.getValue());
            }
            else if (parsedHeaderLine.getHeaderDirective() == HeaderDirective.PERFORMANCE_KEY){
                builder.performanceKey(parsedHeaderLine.getValue());
            }
        }

        return builder.build();
    }
}
