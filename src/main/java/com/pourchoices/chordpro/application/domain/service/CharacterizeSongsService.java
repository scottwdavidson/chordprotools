package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import com.pourchoices.chordpro.application.domain.model.CharacterizeSongsReport;
import com.pourchoices.chordpro.application.domain.model.CharacterizeSongsReport.FieldTally;
import com.pourchoices.chordpro.application.domain.model.CharacterizeSongsReport.RatingField;
import com.pourchoices.chordpro.application.domain.model.SongRating;
import com.pourchoices.chordpro.application.port.in.CharacterizeSongsUseCase;
import com.pourchoices.chordpro.application.port.out.CatalogPort;
import com.pourchoices.chordpro.application.port.out.SongRatingPort;
import com.pourchoices.chordpro.config.ChordproCatalogIndexPathConfig;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Bulk-applies a ratings file (see {@link SongRating}) onto {@code song-catalog.csv},
 * grouped by song identity so one rating covers every key-variant of a song.
 *
 * <p>Never overwrites an already-set field \u2014 checked per field, per catalog
 * row, not per rating row: if a variant was hand-edited with an energyLevel
 * already, that field is left alone even if the rating row also supplies
 * vocalIntensity for the first time on that same variant.
 *
 * <p>{@code fix=false} (the default, mirroring {@code bracket-chords}) never
 * writes {@code song-catalog.csv} \u2014 the report shows exactly what would
 * change so it can be reviewed before committing to it.
 */
@Service
@AllArgsConstructor(onConstructor_ = @__(@Autowired))
@Slf4j
public class CharacterizeSongsService implements CharacterizeSongsUseCase {

    private final CatalogPort catalogPort;
    private final SongRatingPort songRatingPort;
    private final ChordproCatalogIndexPathConfig catalogConfig;

    @Override
    public CharacterizeSongsReport characterizeSongs(String ratingsInputPath, boolean fix) {
        Path catalogPath = Paths.get(catalogConfig.getCatalogIndexPath());
        Map<String, CatalogEntry> catalog = catalogPort.readCatalogFromCsv(catalogPath);
        List<SongRating> ratings = songRatingPort.readRatings(Paths.get(ratingsInputPath));

        Map<String, List<CatalogEntry>> groups = catalog.values().stream()
                .collect(Collectors.groupingBy(e -> e.getSongId().toGroupKey(), LinkedHashMap::new, Collectors.toList()));

        Map<String, CatalogEntry> updated = new LinkedHashMap<>(catalog);
        Map<RatingField, Integer> applied = zeroedCounts();
        Map<RatingField, Integer> skipped = zeroedCounts();
        Set<String> matchedGroupKeys = new HashSet<>();
        List<String> unmatched = new ArrayList<>();

        for (SongRating rating : ratings) {
            List<CatalogEntry> variants = groups.get(rating.getGroupKey());
            if (variants == null || variants.isEmpty()) {
                unmatched.add(rating.getGroupKey());
                continue;
            }
            matchedGroupKeys.add(rating.getGroupKey());
            for (CatalogEntry variant : variants) {
                CatalogEntry merged = applyRating(variant, rating, applied, skipped);
                updated.put(variant.getSongId().toString(), merged);
            }
        }

        List<String> stillUnrated = groups.values().stream()
                .map(variants -> updated.get(variants.get(0).getSongId().toString()))
                .filter(rep -> rep.getEnergyLevel() == null)
                .map(rep -> rep.getTitle() + " \u2014 " + rep.getArtist())
                .sorted()
                .toList();

        if (fix) {
            catalogPort.writeCatalogToCsv(catalogPath, new ArrayList<>(updated.values()));
            log.info("characterize-songs: wrote {} catalog rows", updated.size());
        }

        Map<RatingField, FieldTally> tallies = new EnumMap<>(RatingField.class);
        for (RatingField field : RatingField.values()) {
            tallies.put(field, FieldTally.builder()
                    .applied(applied.get(field))
                    .alreadySetSkipped(skipped.get(field))
                    .build());
        }

        return CharacterizeSongsReport.builder()
                .totalCatalogGroups(groups.size())
                .ratingRowsProvided(ratings.size())
                .ratingRowsMatched(matchedGroupKeys.size())
                .unmatchedRatingGroupKeys(unmatched)
                .stillUnratedGroups(stillUnrated)
                .tallies(tallies)
                .build();
    }

    private CatalogEntry applyRating(CatalogEntry variant, SongRating rating,
                                      Map<RatingField, Integer> applied, Map<RatingField, Integer> skipped) {
        CatalogEntry.CatalogEntryBuilder builder = variant.toBuilder();

        if (rating.getEnergyLevel() != null) {
            if (variant.getEnergyLevel() == null) {
                builder.energyLevel(rating.getEnergyLevel());
                tally(applied, RatingField.ENERGY_LEVEL);
            } else {
                tally(skipped, RatingField.ENERGY_LEVEL);
            }
        }
        if (rating.getVocalIntensity() != null) {
            if (variant.getVocalIntensity() == null) {
                builder.vocalIntensity(rating.getVocalIntensity());
                tally(applied, RatingField.VOCAL_INTENSITY);
            } else {
                tally(skipped, RatingField.VOCAL_INTENSITY);
            }
        }
        if (rating.getGenrePrimary() != null && !rating.getGenrePrimary().isBlank()) {
            if (variant.getGenrePrimary() == null || variant.getGenrePrimary().isBlank()) {
                builder.genrePrimary(rating.getGenrePrimary());
                tally(applied, RatingField.GENRE_PRIMARY);
            } else {
                tally(skipped, RatingField.GENRE_PRIMARY);
            }
        }
        if (rating.getGenreSecondary() != null && !rating.getGenreSecondary().isBlank()) {
            if (variant.getGenreSecondary() == null || variant.getGenreSecondary().isBlank()) {
                builder.genreSecondary(rating.getGenreSecondary());
                tally(applied, RatingField.GENRE_SECONDARY);
            } else {
                tally(skipped, RatingField.GENRE_SECONDARY);
            }
        }
        if (rating.getSingAlong() != null) {
            if (variant.getSingAlong() == null) {
                builder.singAlong(rating.getSingAlong());
                tally(applied, RatingField.SING_ALONG);
            } else {
                tally(skipped, RatingField.SING_ALONG);
            }
        }

        return builder.build();
    }

    private void tally(Map<RatingField, Integer> counts, RatingField field) {
        counts.merge(field, 1, Integer::sum);
    }

    private Map<RatingField, Integer> zeroedCounts() {
        Map<RatingField, Integer> counts = new EnumMap<>(RatingField.class);
        for (RatingField field : RatingField.values()) counts.put(field, 0);
        return counts;
    }
}
