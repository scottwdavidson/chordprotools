package com.pourchoices.chordpro.adapter.out.file;

import com.opencsv.bean.CsvBindByName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;

/**
 * OpenCSV-mapped DTO for one row in {@code setlist-assignments.csv}.
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SetlistAssignmentDto {

    public static final List<String> COLUMN_ORDER =
            Arrays.asList("gig", "song id", "set", "title", "artist");

    @CsvBindByName(column = "gig")
    String gig;

    @CsvBindByName(column = "song id")
    String songId;

    @CsvBindByName(column = "set")
    String set;

    /** Human-readable song title — decorative only, ignored on read. */
    @CsvBindByName(column = "title")
    String title;

    /** Human-readable artist name — decorative only, ignored on read. */
    @CsvBindByName(column = "artist")
    String artist;
}
