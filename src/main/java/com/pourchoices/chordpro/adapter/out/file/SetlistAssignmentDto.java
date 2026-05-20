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
 *
 * <p>Three columns only: GIG, SONG ID, SET. TITLE and ARTIST live in
 * {@code song-catalog.csv} and are joined at runtime — they are not
 * stored here.
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SetlistAssignmentDto {

    public static final List<String> COLUMN_ORDER =
            Arrays.asList("gig", "song id", "set", "rc slot");

    @CsvBindByName(column = "gig")
    String gig;

    @CsvBindByName(column = "song id")
    String songId;

    @CsvBindByName(column = "set")
    String set;

    /** RC-500 slot for this gig. Blank until assign-backing-track-slots runs. Never copied with copy-gig. */
    @CsvBindByName(column = "rc slot")
    String rcSlot;
}
