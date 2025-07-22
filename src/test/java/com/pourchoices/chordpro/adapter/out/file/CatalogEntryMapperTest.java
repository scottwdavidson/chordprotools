package com.pourchoices.chordpro.adapter.out.file;
import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for the CatalogEntryMapper.
 * These tests verify the mapping logic between CatalogEntry and CatalogEntryDto
 * without requiring a Spring application context.
 */
class CatalogEntryMapperTest {

    private final CatalogEntryMapper mapper = Mappers.getMapper(CatalogEntryMapper.class);

    @Test
    void testToDto_SingleObject() {
        // Given
        CatalogEntry entity = CatalogEntry.builder()
                .chordProFilename("/ABC/A/Abba/DancingQueen.cho")
                .title("Test Song Title")
                .artist("Test Artist")
                .key("C")
                .duration("3:30")
                .tempo("120")
                .timeSignature("4/4")
                .capo("0")
                .nord("Yes")
                .version("1.0")
                .countin("4")
                .backing("Full Band")
                .ve("Verse-Chorus")
                .performanceKey("C#")
                .build();

        // When
        CatalogEntryDto dto = mapper.toDto(entity);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.getChordProFilename()).isEqualTo(entity.getChordProFilename());
        assertThat(dto.getTitle()).isEqualTo(entity.getTitle());
        assertThat(dto.getArtist()).isEqualTo(entity.getArtist());
        assertThat(dto.getKey()).isEqualTo(entity.getKey());
        assertThat(dto.getDuration()).isEqualTo(entity.getDuration());
        assertThat(dto.getTempo()).isEqualTo(entity.getTempo());
        assertThat(dto.getTimeSignature()).isEqualTo(entity.getTimeSignature());
        assertThat(dto.getCapo()).isEqualTo(entity.getCapo());
        assertThat(dto.getNord()).isEqualTo(entity.getNord());
        assertThat(dto.getVersion()).isEqualTo(entity.getVersion());
        assertThat(dto.getCountin()).isEqualTo(entity.getCountin());
        assertThat(dto.getBacking()).isEqualTo(entity.getBacking());
        assertThat(dto.getVe()).isEqualTo(entity.getVe());
        assertThat(dto.getPerformanceKey()).isEqualTo(entity.getPerformanceKey());
    }

    @Test
    void testToEntity_SingleObject() {
        // Given
        CatalogEntryDto dto = CatalogEntryDto.builder()
                .chordProFilename("/ABC/A/Another/TestSong.cho")
                .title("Another Test Song")
                .artist("Another Artist")
                .key("G")
                .duration("4:00")
                .tempo("100")
                .timeSignature("3/4")
                .capo("2")
                .nord("No")
                .version("2.1")
                .countin("2")
                .backing("Acoustic")
                .ve("Chorus-Verse")
                .performanceKey("A")
                .build();

        // When
        CatalogEntry entity = mapper.toEntity(dto);

        // Then
        assertThat(entity).isNotNull();
        assertThat(entity.getChordProFilename()).isEqualTo(dto.getChordProFilename());
        assertThat(entity.getTitle()).isEqualTo(dto.getTitle());
        assertThat(entity.getArtist()).isEqualTo(dto.getArtist());
        assertThat(entity.getKey()).isEqualTo(dto.getKey());
        assertThat(entity.getDuration()).isEqualTo(dto.getDuration());
        assertThat(entity.getTempo()).isEqualTo(dto.getTempo());
        assertThat(entity.getTimeSignature()).isEqualTo(dto.getTimeSignature());
        assertThat(entity.getCapo()).isEqualTo(dto.getCapo());
        assertThat(entity.getNord()).isEqualTo(dto.getNord());
        assertThat(entity.getVersion()).isEqualTo(dto.getVersion());
        assertThat(entity.getCountin()).isEqualTo(dto.getCountin());
        assertThat(entity.getBacking()).isEqualTo(dto.getBacking());
        assertThat(entity.getVe()).isEqualTo(dto.getVe());
        assertThat(entity.getPerformanceKey()).isEqualTo(dto.getPerformanceKey());
    }

    @Test
    void testToDtoList() {
        // Given
        List<CatalogEntry> entities = Arrays.asList(
                CatalogEntry.builder().title("Title1").artist("Artist1").key("C").chordProFilename("/ABC/A/Another/TestSong.cho").duration("3:00").build(),
                CatalogEntry.builder().title("Title2").artist("Artist2").key("D").chordProFilename("/ABC/B/BSong.cho").duration("3:00").build()
        );

        // When
        List<CatalogEntryDto> dtoList = mapper.toDtoList(entities);

        // Then
        assertThat(dtoList).isNotNull().hasSize(2);
        assertThat(dtoList.get(1).getTitle()).isEqualTo("Title2");
    }

    @Test
    void testToEntityList() {
        // Given
        List<CatalogEntryDto> dtoList = Arrays.asList(
                CatalogEntryDto.builder().title("DtoTitle1").key("Fm").chordProFilename("ABC/C/ColdPlay/Song").artist("DtoArtist1").duration("3:00").build(),
                CatalogEntryDto.builder().title("DtoTitle2").artist("DtoArtist2").key("Fm").chordProFilename("ABC/C/ColdPlay/Song").duration("4:00").build());

        // When
        List<CatalogEntry> entities = mapper.toEntityList(dtoList);

        // Then
        assertThat(entities).isNotNull().hasSize(2);
        assertThat(entities.get(0).getTitle()).isEqualTo("DtoTitle1");
        assertThat(entities.get(1).getTitle()).isEqualTo("DtoTitle2");
    }

    @Test
    void testToDto_NullInput() {
        // When
        CatalogEntryDto dto = mapper.toDto(null);

        // Then
        assertNull(dto);
    }

    @Test
    void testToEntity_NullInput() {
        // When
        CatalogEntry entity = mapper.toEntity(null);

        // Then
        assertNull(entity);
    }

    @Test
    void testToDtoList_NullInput() {
        // When
        List<CatalogEntryDto> dtoList = mapper.toDtoList(null);

        // Then
        assertNull(dtoList); // MapStruct returns null for null list input by default
    }

    @Test
    void testToEntityList_NullInput() {
        // When
        List<CatalogEntry> entities = mapper.toEntityList(null);

        // Then
        assertNull(entities); // MapStruct returns null for null list input by default
    }

    @Test
    void testToDtoList_EmptyList() {
        // Given
        List<CatalogEntry> entities = List.of(); // Java 9+ immutable empty list

        // When
        List<CatalogEntryDto> dtoList = mapper.toDtoList(entities);

        // Then
        assertThat(dtoList).isNotNull().isEmpty();
    }

    @Test
    void testToEntityList_EmptyList() {
        // Given
        List<CatalogEntryDto> dtoList = List.of(); // Java 9+ immutable empty list

        // When
        List<CatalogEntry> entities = mapper.toEntityList(dtoList);

        // Then
        assertThat(entities).isNotNull().isEmpty();
    }
}