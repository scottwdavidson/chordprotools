package com.pourchoices.chordpro.adapter.out.file;

import com.pourchoices.chordpro.application.domain.model.VenueProfile;
import com.pourchoices.chordpro.application.domain.model.VocalIntensity;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for {@link VenueProfileMapper}. Mirrors the structure of
 * {@code CatalogEntryMapperTest} — same conventions, same domain (opencsv
 * DTO <-> immutable Lombok @Value), same tolerant-parsing contract.
 */
class VenueProfileMapperTest {

    private final VenueProfileMapper mapper = new VenueProfileMapper();

    @Test
    void testToDto_SingleObject() {
        VenueProfile entity = VenueProfile.builder()
                .venue("Blue Vase")
                .maxVocalIntensity(VocalIntensity.NONE)
                .energyCeiling(3)
                .energyFloor(1)
                .build();

        VenueProfileDto dto = mapper.toDto(entity);

        assertThat(dto).isNotNull();
        assertThat(dto.getVenue()).isEqualTo("Blue Vase");
        assertThat(dto.getMaxVocalIntensity()).isEqualTo("NONE");
        assertThat(dto.getEnergyCeiling()).isEqualTo("3");
        assertThat(dto.getEnergyFloor()).isEqualTo("1");
    }

    @Test
    void testToEntity_SingleObject() {
        VenueProfileDto dto = VenueProfileDto.builder()
                .venue("First Friday")
                .maxVocalIntensity("full")
                .energyCeiling("10")
                .energyFloor("2")
                .build();

        VenueProfile entity = mapper.toEntity(dto);

        assertThat(entity).isNotNull();
        assertThat(entity.getVenue()).isEqualTo("First Friday");
        assertThat(entity.getMaxVocalIntensity()).isEqualTo(VocalIntensity.FULL);
        assertThat(entity.getEnergyCeiling()).isEqualTo(10);
        assertThat(entity.getEnergyFloor()).isEqualTo(2);
    }

    @Test
    void testToEntity_UnconfiguredFieldsAreNull() {
        // Given -- a venue row that only sets the vocal constraint, nothing else
        VenueProfileDto dto = VenueProfileDto.builder()
                .venue("DeRose Winery")
                .maxVocalIntensity("LIGHT")
                .build();

        VenueProfile entity = mapper.toEntity(dto);

        assertThat(entity.getMaxVocalIntensity()).isEqualTo(VocalIntensity.LIGHT);
        assertThat(entity.getEnergyCeiling()).isNull();
        assertThat(entity.getEnergyFloor()).isNull();
    }

    @Test
    void testToEntity_MalformedEnergyCeilingBecomesNullNotACrash() {
        VenueProfileDto dto = VenueProfileDto.builder()
                .venue("Typo Venue")
                .energyCeiling("loud")
                .energyFloor("also not a number")
                .build();

        VenueProfile entity = mapper.toEntity(dto);

        assertThat(entity.getEnergyCeiling()).isNull();
        assertThat(entity.getEnergyFloor()).isNull();
    }

    @Test
    void testToEntity_UnrecognisedVocalIntensityBecomesNull() {
        VenueProfileDto dto = VenueProfileDto.builder()
                .venue("Typo Venue")
                .maxVocalIntensity("SCREAMO")
                .build();

        assertThat(mapper.toEntity(dto).getMaxVocalIntensity()).isNull();
    }

    @Test
    void testToDtoList_and_toEntityList_roundTrip() {
        List<VenueProfile> entities = Arrays.asList(
                VenueProfile.builder().venue("Venue A").maxVocalIntensity(VocalIntensity.NONE).energyCeiling(4).build(),
                VenueProfile.builder().venue("Venue B").maxVocalIntensity(VocalIntensity.FULL).energyCeiling(10).build()
        );

        List<VenueProfileDto> dtos = mapper.toDtoList(entities);
        List<VenueProfile> roundTripped = mapper.toEntityList(dtos);

        assertThat(roundTripped).isEqualTo(entities);
    }

    @Test
    void testToDto_NullInput() {
        assertNull(mapper.toDto(null));
    }

    @Test
    void testToEntity_NullInput() {
        assertNull(mapper.toEntity(null));
    }
}
