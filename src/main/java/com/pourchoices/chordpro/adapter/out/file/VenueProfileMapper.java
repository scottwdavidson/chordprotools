package com.pourchoices.chordpro.adapter.out.file;

import com.pourchoices.chordpro.application.domain.model.VenueProfile;
import com.pourchoices.chordpro.application.domain.model.VocalIntensity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Maps between {@link VenueProfile} (domain) and {@link VenueProfileDto} (opencsv).
 *
 * <p>Tolerant integer parsing mirrors {@code CatalogEntryMapper}: a malformed
 * cell in a hand-edited CSV logs a warning and becomes {@code null} rather
 * than breaking the whole read. Not extracted into a shared utility (yet) —
 * two call sites of a few lines each isn't a DRY violation worth the extra
 * indirection; revisit if a third mapper needs the same tolerant-int parse.
 */
@Component
@Slf4j
public class VenueProfileMapper {

    public VenueProfile toEntity(VenueProfileDto dto) {
        if (dto == null) return null;
        return VenueProfile.builder()
                .gig(dto.getGig())
                .maxVocalIntensity(VocalIntensity.fromString(dto.getMaxVocalIntensity()))
                .energyCeiling(parseInt(dto.getEnergyCeiling(), "energy ceiling", dto.getGig()))
                .energyFloor(parseInt(dto.getEnergyFloor(), "energy floor", dto.getGig()))
                .build();
    }

    public VenueProfileDto toDto(VenueProfile entity) {
        if (entity == null) return null;
        return VenueProfileDto.builder()
                .gig(entity.getGig())
                .maxVocalIntensity(entity.getMaxVocalIntensity() != null ? entity.getMaxVocalIntensity().name() : null)
                .energyCeiling(entity.getEnergyCeiling() != null ? entity.getEnergyCeiling().toString() : null)
                .energyFloor(entity.getEnergyFloor() != null ? entity.getEnergyFloor().toString() : null)
                .build();
    }

    public List<VenueProfile> toEntityList(List<VenueProfileDto> dtos) {
        if (dtos == null) return null;
        return dtos.stream().map(this::toEntity).toList();
    }

    public List<VenueProfileDto> toDtoList(List<VenueProfile> entities) {
        if (entities == null) return null;
        return entities.stream().map(this::toDto).toList();
    }

    private Integer parseInt(String value, String fieldLabel, String gig) {
        if (value == null || value.isBlank()) return null;
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            log.warn("{} '{}' for gig '{}' is not a number — treating as unset.", fieldLabel, value, gig);
            return null;
        }
    }
}
