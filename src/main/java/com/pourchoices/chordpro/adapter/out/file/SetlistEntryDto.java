package com.pourchoices.chordpro.adapter.out.file;

import com.opencsv.bean.CsvBindByName;
import java.util.Arrays;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Slim view DTO representing a single row in the exported setlist CSV.
 *
 * <p>Intentionally contains only the four fields relevant for a printed gig setlist.
 * The full song metadata lives in {@link com.pourchoices.chordpro.application.domain.model.CatalogEntry}
 * and is carried by {@link com.pourchoices.chordpro.application.domain.model.Setlist} —
 * this object is purely an output/presentation concern.
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SetlistEntryDto {

    public static final List<String> SETLIST_COLUMN_ORDER =
            Arrays.asList("set", "song title", "song artist", "key");

    @CsvBindByName(column = "set")
    private String set;

    @CsvBindByName(column = "song title")
    private String songTitle;

    @CsvBindByName(column = "song artist")
    private String songArtist;

    @CsvBindByName(column = "key")
    private String key;
}
