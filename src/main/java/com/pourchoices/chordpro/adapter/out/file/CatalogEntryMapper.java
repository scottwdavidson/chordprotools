package com.pourchoices.chordpro.adapter.out.file;

import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Hand-written mapper between CatalogEntryDto (opencsv mutable bean) and
 * CatalogEntry (immutable Lombok @Value domain object).
 *
 * MapStruct was removed: it required a fragile annotation-processor ordering
 * shim (lombok-mapstruct-binding) that behaved non-deterministically across
 * Java 17 patch versions, causing intermittent build failures. For a single
 * flat mapping with identical field names, plain Java is simpler and safer.
 */
@Component
public class CatalogEntryMapper {

    public CatalogEntry toEntity(CatalogEntryDto dto) {
        if (dto == null) return null;
        return CatalogEntry.builder()
                .chordProFilename(dto.getChordProFilename())
                .title(dto.getTitle())
                .artist(dto.getArtist())
                .key(dto.getKey())
                .duration(dto.getDuration())
                .tempo(dto.getTempo())
                .timeSignature(dto.getTimeSignature())
                .capo(dto.getCapo())
                .nord(dto.getNord())
                .roland(dto.getRoland())
                .version(dto.getVersion())
                .countin(dto.getCountin())
                .backing(dto.getBacking())
                .ve(dto.getVe())
                .performanceKey(dto.getPerformanceKey())
                .build();
    }

    public CatalogEntryDto toDto(CatalogEntry entity) {
        if (entity == null) return null;
        return CatalogEntryDto.builder()
                .chordProFilename(entity.getChordProFilename())
                .title(entity.getTitle())
                .artist(entity.getArtist())
                .key(entity.getKey())
                .duration(entity.getDuration())
                .tempo(entity.getTempo())
                .timeSignature(entity.getTimeSignature())
                .capo(entity.getCapo())
                .nord(entity.getNord())
                .roland(entity.getRoland())
                .version(entity.getVersion())
                .countin(entity.getCountin())
                .backing(entity.getBacking())
                .ve(entity.getVe())
                .performanceKey(entity.getPerformanceKey())
                .build();
    }

    public List<CatalogEntry> toEntityList(List<CatalogEntryDto> dtos) {
        if (dtos == null) return null;
        return dtos.stream().map(this::toEntity).toList();
    }

    public List<CatalogEntryDto> toDtoList(List<CatalogEntry> entities) {
        if (entities == null) return null;
        return entities.stream().map(this::toDto).toList();
    }
}
