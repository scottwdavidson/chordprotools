package com.pourchoices.chordpro.adapter.out.file;

import com.pourchoices.chordpro.application.domain.model.SongRating;
import com.pourchoices.chordpro.application.domain.model.VocalIntensity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Maps between {@link SongRating} (domain) and {@link SongRatingDto} (opencsv).
 *
 * <p>Tolerant parsing mirrors {@code CatalogEntryMapper}/{@code VenueProfileMapper}:
 * a malformed cell logs a warning and becomes {@code null} rather than
 * breaking the whole read. This is now the second hand-rolled copy of both
 * the tolerant-int and tolerant-boolean parse (first: {@code CatalogEntryMapper}).
 * Per the same call documented in {@code VenueProfileMapper}, two call sites
 * isn't yet a DRY violation worth the indirection — extract at the third.
 */
@Component
@Slf4j
public class SongRatingMapper {

    public SongRating toEntity(SongRatingDto dto) {
        if (dto == null) return null;
        return SongRating.builder()
                .groupKey(dto.getGroupKey())
                .title(dto.getTitle())
                .artist(dto.getArtist())
                .energyLevel(parseEnergyLevel(dto.getEnergyLevel(), dto.getGroupKey()))
                .vocalIntensity(VocalIntensity.fromString(dto.getVocalIntensity()))
                .genrePrimary(dto.getGenrePrimary())
                .genreSecondary(dto.getGenreSecondary())
                .singAlong(parseSingAlong(dto.getSingAlong(), dto.getGroupKey()))
                .build();
    }

    public List<SongRating> toEntityList(List<SongRatingDto> dtos) {
        if (dtos == null) return null;
        return dtos.stream().map(this::toEntity).toList();
    }

    private Integer parseEnergyLevel(String value, String groupKey) {
        if (value == null || value.isBlank()) return null;
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            log.warn("energy level '{}' for rating '{}' is not a number — treating as unset.", value, groupKey);
            return null;
        }
    }

    private Boolean parseSingAlong(String value, String groupKey) {
        if (value == null || value.isBlank()) return null;
        String v = value.trim().toLowerCase();
        return switch (v) {
            case "true", "yes", "y", "1" -> Boolean.TRUE;
            case "false", "no", "n", "0" -> Boolean.FALSE;
            default -> {
                log.warn("sing along '{}' for rating '{}' is not a recognised boolean — treating as unset.", value, groupKey);
                yield null;
            }
        };
    }
}
