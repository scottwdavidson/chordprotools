package com.pourchoices.chordpro.adapter.out.file;

import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import com.pourchoices.chordpro.application.domain.model.SetlistAssignment;
import com.pourchoices.chordpro.application.domain.model.SongId;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Maps between {@link SetlistAssignment} (domain) and {@link SetlistAssignmentDto} (opencsv).
 */
@Component
public class SetlistAssignmentMapper {

    public SetlistAssignment toEntity(SetlistAssignmentDto dto) {
        if (dto == null) return null;
        SongId songId = SongId.parse(dto.getSongId());
        if (!songId.isBaseVersion()) {
            throw new IllegalArgumentException(
                    "setlist-assignments.csv contains a key-variant SONG ID: '" + dto.getSongId()
                    + "'. Setlist assignments must reference base versions only (no key suffix). "
                    + "Change it to: '" + songId.toGroupKey() + "'");
        }
        return SetlistAssignment.builder()
                .gig(dto.getGig())
                .songId(songId)
                .set(dto.getSet())
                .build();
    }

    /** Maps to a bare DTO (no title/artist). */
    public SetlistAssignmentDto toDto(SetlistAssignment entity) {
        if (entity == null) return null;
        return SetlistAssignmentDto.builder()
                .gig(entity.getGig())
                .songId(entity.getSongId().toString())
                .set(entity.getSet())
                .build();
    }

    /**
     * Maps to an enriched DTO with TITLE and ARTIST populated from the catalog.
     * If the song is not found in the catalog, those fields are left blank.
     */
    public SetlistAssignmentDto toEnrichedDto(SetlistAssignment entity,
                                              Map<String, CatalogEntry> catalog) {
        if (entity == null) return null;
        CatalogEntry song = catalog.get(entity.getSongId().toString());
        return SetlistAssignmentDto.builder()
                .gig(entity.getGig())
                .songId(entity.getSongId().toString())
                .set(entity.getSet())
                .title(song != null ? song.getTitle() : "")
                .artist(song != null ? song.getArtist() : "")
                .build();
    }

    public List<SetlistAssignment> toEntityList(List<SetlistAssignmentDto> dtos) {
        if (dtos == null) return null;
        return dtos.stream().map(this::toEntity).toList();
    }

    public List<SetlistAssignmentDto> toDtoList(List<SetlistAssignment> entities) {
        if (entities == null) return null;
        return entities.stream().map(this::toDto).toList();
    }

    public List<SetlistAssignmentDto> toEnrichedDtoList(List<SetlistAssignment> entities,
                                                        Map<String, CatalogEntry> catalog) {
        if (entities == null) return null;
        return entities.stream().map(e -> toEnrichedDto(e, catalog)).toList();
    }
}
