package com.pourchoices.chordpro.adapter.out.file;

import com.opencsv.bean.CsvToBeanBuilder;
import com.pourchoices.chordpro.application.domain.model.SongRating;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Reads a {@code characterize-songs} ratings input file into {@link SongRating} rows.
 */
@Service
@Slf4j
public class SongRatingFileReader {

    private final SongRatingMapper mapper;

    public SongRatingFileReader(SongRatingMapper mapper) {
        this.mapper = mapper;
    }

    @SneakyThrows
    public List<SongRating> readRatings(Path path) {
        if (!Files.exists(path)) {
            log.warn("Ratings input file not found at {} — nothing to apply.", path);
            return List.of();
        }

        log.info("Reading song ratings from {}", path);

        List<SongRatingDto> dtos;
        try (Reader reader = Files.newBufferedReader(path.toAbsolutePath())) {
            dtos = new CsvToBeanBuilder<SongRatingDto>(reader)
                    .withType(SongRatingDto.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build()
                    .parse();
        }

        List<SongRating> ratings = mapper.toEntityList(dtos);
        log.info("Read {} song rating(s)", ratings.size());
        return ratings;
    }
}
