package com.pourchoices.chordpro.application.domain.model;

import com.pourchoices.chordpro.adapter.out.file.CatalogEntryDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ParsedHeaderMapper {


    public ParsedHeader fromCatalogEntry(CatalogEntry catalogEntry) {

        ParsedHeader.ParsedHeaderBuilder parsedHeaderBuilder = ParsedHeader.builder();

        // mandatory entries
        parsedHeaderBuilder.headerLine(
                ParsedHeaderLine.builder()
                        .headerDirective(HeaderDirective.TITLE)
                        .value(catalogEntry.getTitle())
                        .build());

        parsedHeaderBuilder.headerLine(
                ParsedHeaderLine.builder()
                        .headerDirective(HeaderDirective.ARTIST)
                        .value(catalogEntry.getArtist())
                        .build());

        parsedHeaderBuilder.headerLine(
                ParsedHeaderLine.builder()
                        .headerDirective(HeaderDirective.KEY)
                        .value(catalogEntry.getKey())
                        .build());

        parsedHeaderBuilder.headerLine(
                ParsedHeaderLine.builder()
                        .headerDirective(HeaderDirective.DURATION)
                        .value(catalogEntry.getDuration())
                        .build());

        parsedHeaderBuilder.headerLine(
                ParsedHeaderLine.builder()
                        .headerDirective(HeaderDirective.TEMPO)
                        .value(catalogEntry.getTempo())
                        .build());

        // optional
        if (null != catalogEntry.getCapo() && !catalogEntry.getCapo().isBlank()) {
            parsedHeaderBuilder.headerLine(
                    ParsedHeaderLine.builder()
                            .headerDirective(HeaderDirective.CAPO)
                            .value(catalogEntry.getCapo())
                            .build());
        }

        if (null != catalogEntry.getTimeSignature() && !catalogEntry.getTimeSignature().isBlank()) {
            parsedHeaderBuilder.headerLine(
                    ParsedHeaderLine.builder()
                            .headerDirective(HeaderDirective.TIME_SIGNATURE)
                            .value(catalogEntry.getTimeSignature())
                            .build());
        }

        if (null != catalogEntry.getCountin() && !catalogEntry.getCountin().isBlank()) {
            parsedHeaderBuilder.headerLine(
                    ParsedHeaderLine.builder()
                            .headerDirective(HeaderDirective.COUNTIN)
                            .value(catalogEntry.getCountin())
                            .build());
        }

        if (null != catalogEntry.getNord() && !catalogEntry.getNord().isBlank()) {
            parsedHeaderBuilder.headerLine(
                    ParsedHeaderLine.builder()
                            .headerDirective(HeaderDirective.NORD)
                            .value(catalogEntry.getNord())
                            .build());
        }

        if (null != catalogEntry.getBacking() && !catalogEntry.getBacking().isBlank()) {
            parsedHeaderBuilder.headerLine(
                    ParsedHeaderLine.builder()
                            .headerDirective(HeaderDirective.BACKING)
                            .value(catalogEntry.getBacking())
                            .build());
        }

        if (null != catalogEntry.getVe() && !catalogEntry.getVe().isBlank()) {
            parsedHeaderBuilder.headerLine(
                    ParsedHeaderLine.builder()
                            .headerDirective(HeaderDirective.VE)
                            .value(catalogEntry.getVe())
                            .build());
        }

        if (null != catalogEntry.getVersion() && !catalogEntry.getVersion().isBlank()) {
            parsedHeaderBuilder.headerLine(
                    ParsedHeaderLine.builder()
                            .headerDirective(HeaderDirective.VERSION)
                            .value(catalogEntry.getVersion())
                            .build());
        }

        return parsedHeaderBuilder.build();
    }
}
