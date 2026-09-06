package com.pourchoices.chordpro.adapter.out.file;

import com.opencsv.bean.CsvToBeanBuilder;
import com.pourchoices.chordpro.application.domain.model.VenueProfile;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Reads {@code venue-profiles.csv} into a gig-keyed map of {@link VenueProfile}.
 */
@Service
@Slf4j
public class VenueProfileFileReader {

    private final VenueProfileMapper mapper;

    VenueProfileFileReader(VenueProfileMapper mapper) {
        this.mapper = mapper;
    }

    @SneakyThrows
    public Map<String, VenueProfile> readVenueProfiles(Path path) {
        if (!Files.exists(path)) {
            log.info("venue-profiles.csv not found at {} — treating as \"no venue profiles configured.\"", path);
            return Map.of();
        }

        log.info("Reading venue profiles from {}", path);

        List<VenueProfileDto> dtos;
        try (Reader reader = Files.newBufferedReader(path.toAbsolutePath())) {
            dtos = new CsvToBeanBuilder<VenueProfileDto>(reader)
                    .withType(VenueProfileDto.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build()
                    .parse();
        }

        List<VenueProfile> profiles = mapper.toEntityList(dtos);
        log.info("Read {} venue profile(s)", profiles.size());

        return profiles.stream()
                .collect(Collectors.toUnmodifiableMap(VenueProfile::getVenue, p -> p));
    }
}
