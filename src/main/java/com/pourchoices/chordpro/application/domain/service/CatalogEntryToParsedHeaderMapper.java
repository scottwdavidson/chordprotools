package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import com.pourchoices.chordpro.application.domain.model.BackingType;
import com.pourchoices.chordpro.application.domain.model.ChordProPath;
import com.pourchoices.chordpro.application.domain.model.HeaderDirective;
import com.pourchoices.chordpro.application.domain.model.ParsedHeader;
import com.pourchoices.chordpro.application.domain.model.ParsedHeaderLine;
import org.springframework.stereotype.Component;

/**
 * Maps a {@link CatalogEntry} (domain) to a {@link ParsedHeader} (domain).
 * Mandatory fields are always added; optional fields are skipped when blank or null.
 */
@Component
public class CatalogEntryToParsedHeaderMapper {

    public ParsedHeader fromCatalogEntry(CatalogEntry entry) {

        ParsedHeader.ParsedHeaderBuilder builder = ParsedHeader.builder()
                .chordProFilename(ChordProPath.toFilePath(entry.getSongId()));

        // mandatory
        builder.headerLine(lineFor(HeaderDirective.TITLE,    entry.getTitle()));
        builder.headerLine(lineFor(HeaderDirective.ARTIST,   entry.getArtist()));
        builder.headerLine(lineFor(HeaderDirective.KEY,      entry.getKey()));
        builder.headerLine(lineFor(HeaderDirective.DURATION, entry.getDuration()));
        builder.headerLine(lineFor(HeaderDirective.TEMPO,    entry.getTempo()));

        // optional — only added when the field has a real value
        addIfPresent(builder, HeaderDirective.CAPO,            entry.getCapo());
        addIfPresent(builder, HeaderDirective.TIME_SIGNATURE,  entry.getTimeSignature());
        addIfPresent(builder, HeaderDirective.COUNTIN,         entry.getCountin());
        addIfPresent(builder, HeaderDirective.NORD,            entry.getNord());
        addIfPresent(builder, HeaderDirective.ROLAND,          entry.getRoland());
        // BACKING = device type (RC or BB); RC_SLOT is written by assign-backing-track-slots
        // and preserved by UpdateSongService — it is NOT a catalog property
        if (entry.getBackingType() != null) {
            builder.headerLine(lineFor(HeaderDirective.BACKING, entry.getBackingType().name()));
        }
        addIfPresent(builder, HeaderDirective.SONG_LABEL,       entry.getSongLabel());
        addIfPresent(builder, HeaderDirective.VE,              entry.getVe());
        addIfPresent(builder, HeaderDirective.PERFORMANCE_KEY, entry.getPerformanceKey());

        return builder.build();
    }

    // --- helpers ---

    private void addIfPresent(ParsedHeader.ParsedHeaderBuilder builder,
                              HeaderDirective directive,
                              String value) {
        if (hasValue(value)) {
            builder.headerLine(lineFor(directive, value));
        }
    }

    private static ParsedHeaderLine lineFor(HeaderDirective directive, String value) {
        return ParsedHeaderLine.builder()
                .headerDirective(directive)
                .value(value)
                .build();
    }

    private static boolean hasValue(String value) {
        return value != null && !value.isBlank();
    }
}
