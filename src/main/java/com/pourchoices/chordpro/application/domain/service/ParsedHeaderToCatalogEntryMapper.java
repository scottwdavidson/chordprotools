package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.BackingType;
import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import com.pourchoices.chordpro.application.domain.model.ChordProPath;
import com.pourchoices.chordpro.application.domain.model.HeaderDirective;
import com.pourchoices.chordpro.application.domain.model.ParsedHeader;
import com.pourchoices.chordpro.application.domain.model.ParsedHeaderLine;
import com.pourchoices.chordpro.application.domain.model.SongId;
import org.springframework.stereotype.Component;

/**
 * Maps a {@link ParsedHeader} (from a parsed {@code .cho} file) into a
 * {@link CatalogEntry} domain object.
 *
 * <p>This is the inverse of {@link CatalogEntryToParsedHeaderMapper} and
 * replaces the private {@code toCatalogEntry()} method that used to live
 * inside {@code GenerateSongCatalogService}.
 *
 * <p>SONG ID is always derived from the {@code .cho} file path — it is
 * never stored inside the file itself.
 */
@Component
public class ParsedHeaderToCatalogEntryMapper {

    /**
     * Converts a parsed {@code .cho} file header into a {@link CatalogEntry}.
     *
     * @param chordproFilename the file path from which the header was parsed;
     *                         used to derive the SONG ID
     * @param parsedHeader     the header parsed from that file
     * @return a fully-populated {@link CatalogEntry}, or {@code null} if the
     *         header is empty (i.e. the file had no recognisable directives)
     */
    public CatalogEntry toCatalogEntry(String chordproFilename, ParsedHeader parsedHeader) {

        if (parsedHeader.getHeaderLines().isEmpty()) {
            return null;
        }

        SongId songId = ChordProPath.toSongId(chordproFilename);

        CatalogEntry.CatalogEntryBuilder builder = CatalogEntry.builder()
                .songId(songId)
                .title("")
                .artist("")
                .key("")
                .duration("");

        for (ParsedHeaderLine line : parsedHeader.getHeaderLines()) {
            HeaderDirective d = line.getHeaderDirective();
            String v = line.getValue();

            if      (d == HeaderDirective.TITLE)           builder.title(v);
            else if (d == HeaderDirective.ARTIST)          builder.artist(v);
            else if (d == HeaderDirective.KEY)             builder.key(v);
            else if (d == HeaderDirective.DURATION)        builder.duration(v);
            else if (d == HeaderDirective.TEMPO)           builder.tempo(v);
            else if (d == HeaderDirective.TIME_SIGNATURE)  builder.timeSignature(v);
            else if (d == HeaderDirective.CAPO)            builder.capo(v);
            else if (d == HeaderDirective.NORD)            builder.nord(v);
            else if (d == HeaderDirective.ROLAND)          builder.roland(v);
            else if (d == HeaderDirective.COUNTIN)         builder.countin(v);
            else if (d == HeaderDirective.BACKING)         builder.backingType(BackingType.fromString(v));
            else if (d == HeaderDirective.RC_SLOT)         builder.rcSlot(v);
            else if (d == HeaderDirective.SONG_LABEL)      builder.songLabel(v);
            else if (d == HeaderDirective.VE)              builder.ve(v);
            else if (d == HeaderDirective.PERFORMANCE_KEY) builder.performanceKey(v);
        }

        return builder.build();
    }
}
