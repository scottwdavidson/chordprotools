package com.pourchoices.chordpro.adapter.out.file;

import com.pourchoices.chordpro.application.domain.model.BackingType;
import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import com.pourchoices.chordpro.application.domain.model.SongId;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class CatalogEntryMapper {

    private static final int SONG_LABEL_MAX_LENGTH = 12;

    public CatalogEntry toEntity(CatalogEntryDto dto) {
        if (dto == null) return null;

        if (dto.getSongLabel() != null && dto.getSongLabel().length() > SONG_LABEL_MAX_LENGTH) {
            log.warn("Song label '{}' for song '{}' exceeds the RC-500 limit of {} characters — "
                    + "it will display truncated on the hardware.",
                    dto.getSongLabel(), dto.getSongId(), SONG_LABEL_MAX_LENGTH);
        }

        return CatalogEntry.builder()
                .songId(SongId.parse(dto.getSongId()))
                .title(dto.getTitle())
                .artist(dto.getArtist())
                .key(dto.getKey())
                .duration(dto.getDuration())
                .tempo(dto.getTempo())
                .timeSignature(dto.getTimeSignature())
                .capo(dto.getCapo())
                .nord(dto.getNord())
                .roland(dto.getRoland())
                .countin(dto.getCountin())
                .backingType(BackingType.fromString(dto.getBacking()))
                .ve(dto.getVe())
                .performanceKey(dto.getPerformanceKey())
                .songLabel(dto.getSongLabel())
                .build();
    }

    public CatalogEntryDto toDto(CatalogEntry entity) {
        if (entity == null) return null;
        return CatalogEntryDto.builder()
                .songId(entity.getSongId().toString())
                .title(entity.getTitle())
                .artist(entity.getArtist())
                .key(entity.getKey())
                .duration(entity.getDuration())
                .tempo(entity.getTempo())
                .timeSignature(entity.getTimeSignature())
                .capo(entity.getCapo())
                .nord(entity.getNord())
                .roland(entity.getRoland())
                .countin(entity.getCountin())
                .backing(entity.getBackingType() != null ? entity.getBackingType().name() : null)
                .ve(entity.getVe())
                .performanceKey(entity.getPerformanceKey())
                .songLabel(entity.getSongLabel())
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
