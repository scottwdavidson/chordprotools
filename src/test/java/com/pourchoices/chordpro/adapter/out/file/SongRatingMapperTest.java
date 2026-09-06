package com.pourchoices.chordpro.adapter.out.file;

import com.pourchoices.chordpro.application.domain.model.SongRating;
import com.pourchoices.chordpro.application.domain.model.VocalIntensity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for {@link SongRatingMapper}, mirroring {@code VenueProfileMapperTest}.
 */
class SongRatingMapperTest {

    private final SongRatingMapper mapper = new SongRatingMapper();

    @Test
    void toEntity_fullRow() {
        SongRatingDto dto = SongRatingDto.builder()
                .groupKey("ABC:B:TomPetty:YouWreckMe")
                .title("You Wreck Me")
                .artist("Tom Petty")
                .energyLevel("8")
                .vocalIntensity("full")
                .genrePrimary("Rock")
                .genreSecondary("Classic Rock")
                .singAlong("yes")
                .build();

        SongRating rating = mapper.toEntity(dto);

        assertThat(rating.getGroupKey()).isEqualTo("ABC:B:TomPetty:YouWreckMe");
        assertThat(rating.getEnergyLevel()).isEqualTo(8);
        assertThat(rating.getVocalIntensity()).isEqualTo(VocalIntensity.FULL);
        assertThat(rating.getGenrePrimary()).isEqualTo("Rock");
        assertThat(rating.getSingAlong()).isTrue();
    }

    @Test
    void toEntity_malformedEnergyLevel_becomesNullNotACrash() {
        SongRatingDto dto = SongRatingDto.builder().groupKey("g").energyLevel("loud").build();

        assertThat(mapper.toEntity(dto).getEnergyLevel()).isNull();
    }

    @Test
    void toEntity_malformedSingAlong_becomesNull() {
        SongRatingDto dto = SongRatingDto.builder().groupKey("g").singAlong("maybe").build();

        assertThat(mapper.toEntity(dto).getSingAlong()).isNull();
    }

    @Test
    void toEntity_blankFields_becomeNull() {
        SongRatingDto dto = SongRatingDto.builder().groupKey("g").build();

        SongRating rating = mapper.toEntity(dto);

        assertThat(rating.getEnergyLevel()).isNull();
        assertThat(rating.getVocalIntensity()).isNull();
        assertThat(rating.getSingAlong()).isNull();
    }

    @Test
    void toEntity_nullInput() {
        assertNull(mapper.toEntity(null));
    }
}
