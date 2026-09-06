package com.pourchoices.chordpro.adapter.out.file;

import com.opencsv.bean.CsvBindByName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;

/**
 * OpenCSV-mapped DTO for one row in a {@code characterize-songs} ratings
 * input file. See {@code SongRating} for why it's keyed by group key rather
 * than title/artist.
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SongRatingDto {

    public static final List<String> COLUMN_ORDER = Arrays.asList(
            "song group key", "title", "artist",
            "energy level", "vocal intensity", "genre primary", "genre secondary", "sing along");

    @CsvBindByName(column = "song group key")
    String groupKey;
    @CsvBindByName(column = "title")
    String title;
    @CsvBindByName(column = "artist")
    String artist;
    @CsvBindByName(column = "energy level")
    String energyLevel;
    @CsvBindByName(column = "vocal intensity")
    String vocalIntensity;
    @CsvBindByName(column = "genre primary")
    String genrePrimary;
    @CsvBindByName(column = "genre secondary")
    String genreSecondary;
    @CsvBindByName(column = "sing along")
    String singAlong;
}
