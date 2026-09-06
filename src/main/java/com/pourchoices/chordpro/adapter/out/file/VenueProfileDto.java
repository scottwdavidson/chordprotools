package com.pourchoices.chordpro.adapter.out.file;

import com.opencsv.bean.CsvBindByName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;

/**
 * OpenCSV-mapped DTO for one row in {@code venue-profiles.csv}.
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VenueProfileDto {

    public static final List<String> COLUMN_ORDER =
            Arrays.asList("gig", "max vocal intensity", "energy ceiling", "energy floor");

    @CsvBindByName(column = "gig")
    String gig;

    @CsvBindByName(column = "max vocal intensity")
    String maxVocalIntensity;

    @CsvBindByName(column = "energy ceiling")
    String energyCeiling;

    @CsvBindByName(column = "energy floor")
    String energyFloor;
}
