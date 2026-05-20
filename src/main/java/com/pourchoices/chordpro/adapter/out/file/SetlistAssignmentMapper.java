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
                .rcSlot(blankToNull(dto.getRcSlot()))
                .build();
    }

    public SetlistAssignmentDto toDto(SetlistAssignment entity) {
        if (entity == null) return null;
        return SetlistAssignmentDto.builder()
                .gig(entity.getGig())
                .songId(entity.getSongId().toString())
                .set(entity.getSet())
                .rcSlot(entity.getRcSlot() != null ? entity.getRcSlot() : "")
                .build();
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
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
