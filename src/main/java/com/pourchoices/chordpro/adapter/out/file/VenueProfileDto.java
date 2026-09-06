package com.pourchoices.chordpro.adapter.out.file;

import com.opencsv.bean.CsvBindByName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;

/**
 * OpenCSV-mapped DTO for one row in {@code venue-profiles.csv}. Keyed by
 * venue name (see {@code VenueProfile} for why).
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VenueProfileDto {

    public static final List<String> COLUMN_ORDER =
            Arrays.asList("venue", "max vocal intensity", "energy ceiling", "energy floor");

    @CsvBindByName(column = "venue")
    String venue;

    @CsvBindByName(column = "max vocal intensity")
    String maxVocalIntensity;

    @CsvBindByName(column = "energy ceiling")
    String energyCeiling;

    @CsvBindByName(column = "energy floor")
    String energyFloor;
}
