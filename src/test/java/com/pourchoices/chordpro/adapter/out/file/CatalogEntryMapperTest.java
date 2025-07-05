package com.pourchoices.chordpro.adapter.out.file;

import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class CatalogEntryMapperTest {

    private final CatalogEntryMapper mapper = CatalogEntryMapper.INSTANCE;

    @Test
    void testToCatalogEntry() {
        // Given
        CatalogEntryDto dto = CatalogEntryDto.builder()
                .chordProFilename("song1.crd")
                .title("Amazing Grace")
                .artist("John Newton")
                .key("G")
                .duration("3:00")
                .tempo("80")
                .timeSignature("4/4")
                .capo("0")
                .nord("Piano")
                .version("1.0")
                .countin("4")
                .backing("Yes")
                .ve("Acoustic")
                .performanceKey("G")
                .build();

        // When
        CatalogEntry entry = mapper.toCatalogEntry(dto);

        // Then
        assertThat(entry).isNotNull();
        assertThat(entry.getChordProFilename()).isEqualTo(dto.getChordProFilename());
        assertThat(entry.getTitle()).isEqualTo(dto.getTitle());
        assertThat(entry.getArtist()).isEqualTo(dto.getArtist());
        assertThat(entry.getKey()).isEqualTo(dto.getKey());
        assertThat(entry.getDuration()).isEqualTo(dto.getDuration());
        assertThat(entry.getTempo()).isEqualTo(dto.getTempo());
        assertThat(entry.getTimeSignature()).isEqualTo(dto.getTimeSignature());
        assertThat(entry.getCapo()).isEqualTo(dto.getCapo());
        assertThat(entry.getNord()).isEqualTo(dto.getNord());
        assertThat(entry.getVersion()).isEqualTo(dto.getVersion());
        assertThat(entry.getCountin()).isEqualTo(dto.getCountin());
        assertThat(entry.getBacking()).isEqualTo(dto.getBacking());
        assertThat(entry.getVe()).isEqualTo(dto.getVe());
        assertThat(entry.getPerformanceKey()).isEqualTo(dto.getPerformanceKey());
    }

    @Test
    void testToCatalogEntryDto() {
        // Given
        CatalogEntry entry = CatalogEntry.builder()
                .chordProFilename("song1.crd")
                .title("Amazing Grace")
                .artist("John Newton")
                .key("G")
                .duration("3:00")
                .tempo("80")
                .timeSignature("4/4")
                .capo("0")
                .nord("Piano")
                .version("1.0")
                .countin("4")
                .backing("Yes")
                .ve("Acoustic")
                .performanceKey("G")
                .build();

        // When
        CatalogEntryDto dto = mapper.toCatalogEntryDto(entry);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.getChordProFilename()).isEqualTo(entry.getChordProFilename());
        assertThat(dto.getTitle()).isEqualTo(entry.getTitle());
        assertThat(dto.getArtist()).isEqualTo(entry.getArtist());
        assertThat(dto.getKey()).isEqualTo(entry.getKey());
        assertThat(dto.getDuration()).isEqualTo(entry.getDuration());
        assertThat(dto.getTempo()).isEqualTo(entry.getTempo());
        assertThat(dto.getTimeSignature()).isEqualTo(entry.getTimeSignature());
        assertThat(dto.getCapo()).isEqualTo(entry.getCapo());
        assertThat(dto.getNord()).isEqualTo(entry.getNord());
        assertThat(dto.getVersion()).isEqualTo(entry.getVersion());
        assertThat(dto.getCountin()).isEqualTo(entry.getCountin());
        assertThat(dto.getBacking()).isEqualTo(entry.getBacking());
        assertThat(dto.getVe()).isEqualTo(entry.getVe());
        assertThat(dto.getPerformanceKey()).isEqualTo(entry.getPerformanceKey());
    }
}
