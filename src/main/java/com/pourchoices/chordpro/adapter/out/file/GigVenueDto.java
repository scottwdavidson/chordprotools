package com.pourchoices.chordpro.adapter.out.file;

import com.opencsv.bean.CsvBindByName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;

/**
 * OpenCSV-mapped DTO for one row in {@code gig-venues.csv}. Two columns
 * only — this file is a pure association, not a rich entity (see
 * {@code GigVenuePort} javadoc for why there's no domain-model counterpart).
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GigVenueDto {

    public static final List<String> COLUMN_ORDER = Arrays.asList("gig", "venue");

    @CsvBindByName(column = "gig")
    String gig;

    @CsvBindByName(column = "venue")
    String venue;
}
