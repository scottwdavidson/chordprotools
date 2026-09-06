package com.pourchoices.chordpro.adapter.out.file;

import com.opencsv.bean.CsvToBeanBuilder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads {@code gig-venues.csv} into a gig-keyed map of venue names.
 */
@Service
@Slf4j
public class GigVenueFileReader {

    @SneakyThrows
    public Map<String, String> readGigVenues(Path path) {
        if (!Files.exists(path)) {
            log.info("gig-venues.csv not found at {} — treating as \"no gig-venue associations configured.\"", path);
            return Map.of();
        }

        log.info("Reading gig-venue associations from {}", path);

        List<GigVenueDto> dtos;
        try (Reader reader = Files.newBufferedReader(path.toAbsolutePath())) {
            dtos = new CsvToBeanBuilder<GigVenueDto>(reader)
                    .withType(GigVenueDto.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build()
                    .parse();
        }

        Map<String, String> gigToVenue = new LinkedHashMap<>();
        for (GigVenueDto dto : dtos) {
            if (dto.getGig() == null || dto.getGig().isBlank()) continue;
            String gig = dto.getGig().trim();
            String venue = dto.getVenue() == null ? null : dto.getVenue().trim();
            if (gigToVenue.containsKey(gig)) {
                log.warn("gig-venues.csv has more than one row for gig '{}' — keeping the first ('{}'), ignoring '{}'.",
                        gig, gigToVenue.get(gig), venue);
                continue;
            }
            gigToVenue.put(gig, venue);
        }

        log.info("Read {} gig-venue association(s)", gigToVenue.size());
        return Map.copyOf(gigToVenue);
    }
}
