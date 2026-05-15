package com.pourchoices.chordpro.adapter.out.file;

import com.pourchoices.chordpro.application.domain.model.SetlistAssignment;
import com.pourchoices.chordpro.application.domain.model.SongId;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Maps between {@link SetlistAssignment} (domain) and {@link SetlistAssignmentDto} (opencsv).
 */
@Component
public class SetlistAssignmentMapper {

    public SetlistAssignment toEntity(SetlistAssignmentDto dto) {
        if (dto == null) return null;
        return SetlistAssignment.builder()
                .gig(dto.getGig())
                .songId(SongId.parse(dto.getSongId()))
                .set(dto.getSet())
                .build();
    }

    public SetlistAssignmentDto toDto(SetlistAssignment entity) {
        if (entity == null) return null;
        return SetlistAssignmentDto.builder()
                .gig(entity.getGig())
                .songId(entity.getSongId().toString())
                .set(entity.getSet())
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
}
