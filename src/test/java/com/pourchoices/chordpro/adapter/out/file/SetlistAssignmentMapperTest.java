package com.pourchoices.chordpro.adapter.out.file;

import com.pourchoices.chordpro.application.domain.model.SetlistAssignment;
import com.pourchoices.chordpro.application.domain.model.SongId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNull;

class SetlistAssignmentMapperTest {

    private final SetlistAssignmentMapper mapper = new SetlistAssignmentMapper();

    @Test
    void toDto_mapsAllFields() {
        SetlistAssignment entity = SetlistAssignment.builder()
                .gig("2026-06-14-rusty-nail")
                .songId(SongId.parse("DEF:E:EltonJohn:Daniel"))
                .set("A01")
                .build();

        SetlistAssignmentDto dto = mapper.toDto(entity);

        assertThat(dto.getGig()).isEqualTo("2026-06-14-rusty-nail");
        assertThat(dto.getSongId()).isEqualTo("DEF:E:EltonJohn:Daniel");
        assertThat(dto.getSet()).isEqualTo("A01");
    }

    @Test
    void toEntity_mapsAllFields() {
        SetlistAssignmentDto dto = SetlistAssignmentDto.builder()
                .gig("2025-10-12-theatre")
                .songId("ABC:B:BillyJoel:MyLife")   // base version — no key suffix
                .set("B10")
                .build();

        SetlistAssignment entity = mapper.toEntity(dto);

        assertThat(entity.getGig()).isEqualTo("2025-10-12-theatre");
        assertThat(entity.getSongId().toString()).isEqualTo("ABC:B:BillyJoel:MyLife");
        assertThat(entity.getSongId().isBaseVersion()).isTrue();
        assertThat(entity.getSet()).isEqualTo("B10");
    }

    @Test
    void toEntity_variantSongId_throwsWithHelpfulMessage() {
        // Setlist assignments must reference base versions only.
        // A variant like MyLife-c in the CSV is a data-entry error.
        SetlistAssignmentDto dto = SetlistAssignmentDto.builder()
                .gig("2025-10-12-theatre")
                .songId("ABC:B:BillyJoel:MyLife-c")  // key variant — illegal
                .set("B10")
                .build();

        assertThatThrownBy(() -> mapper.toEntity(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ABC:B:BillyJoel:MyLife-c")
                .hasMessageContaining("ABC:B:BillyJoel:MyLife"); // suggests the fix
    }

    @Test
    void toDto_nullInput_returnsNull() {
        assertNull(mapper.toDto(null));
    }

    @Test
    void toEntity_nullInput_returnsNull() {
        assertNull(mapper.toEntity(null));
    }

    @Test
    void roundTrip_entityToDtoAndBack_isEqual() {
        SetlistAssignment original = SetlistAssignment.builder()
                .gig("2026-06-14-rusty-nail")
                .songId(SongId.parse("STU:T:TravelingWilburys:HandleWithCare"))
                .set("A03")
                .build();

        SetlistAssignment roundTripped = mapper.toEntity(mapper.toDto(original));

        assertThat(roundTripped.getGig()).isEqualTo(original.getGig());
        assertThat(roundTripped.getSongId()).isEqualTo(original.getSongId());
        assertThat(roundTripped.getSet()).isEqualTo(original.getSet());
    }

    @Test
    void toDtoList_mapsAllElements() {
        List<SetlistAssignment> entities = List.of(
                SetlistAssignment.builder().gig("gig-a").songId(SongId.parse("ABC:A:Abba:DoesYourMotherKnow")).set("A01").build(),
                SetlistAssignment.builder().gig("gig-a").songId(SongId.parse("DEF:E:EltonJohn:Daniel")).set("A02").build()
        );

        List<SetlistAssignmentDto> dtos = mapper.toDtoList(entities);

        assertThat(dtos).hasSize(2);
        assertThat(dtos.get(0).getGig()).isEqualTo("gig-a");
        assertThat(dtos.get(1).getSongId()).isEqualTo("DEF:E:EltonJohn:Daniel");
    }

    @Test
    void toEntityList_mapsAllElements() {
        List<SetlistAssignmentDto> dtos = List.of(
                SetlistAssignmentDto.builder().gig("gig-b").songId("ABC:B:BillyJoel:MyLife").set("A01").build(),
                SetlistAssignmentDto.builder().gig("gig-b").songId("STU:S:SealsCrofts:DiamondGirl").set("A02").build()
        );

        List<SetlistAssignment> entities = mapper.toEntityList(dtos);

        assertThat(entities).hasSize(2);
        assertThat(entities.get(0).getSet()).isEqualTo("A01");
        assertThat(entities.get(1).getSongId().toString()).isEqualTo("STU:S:SealsCrofts:DiamondGirl");
    }

    @Test
    void toDtoList_nullInput_returnsNull() {
        assertNull(mapper.toDtoList(null));
    }

    @Test
    void toEntityList_nullInput_returnsNull() {
        assertNull(mapper.toEntityList(null));
    }
}
