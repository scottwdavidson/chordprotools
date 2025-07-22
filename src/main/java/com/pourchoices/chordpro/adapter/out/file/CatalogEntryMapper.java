package com.pourchoices.chordpro.adapter.out.file;

import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping; // Potentially needed for custom mappings
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring") // Or "default" if not using Spring
public interface CatalogEntryMapper {

//    static final CatalogEntryMapper INSTANCE = Mappers.getMapper(CatalogEntryMapper.class);

    // For single DTO to Entity conversion
    // MapStruct will use the all-args constructor of CatalogEntry
    // and match DTO field names to constructor parameter names.
    CatalogEntry toEntity(CatalogEntryDto dto);

    // For single Entity to DTO conversion (if DTO is also immutable or has setters)
    // If CatalogEntryDto also uses @Value/@Builder, this works similarly.
    // If CatalogEntryDto uses @Data, MapStruct uses setters.
    CatalogEntryDto toDto(CatalogEntry entity);

    // For list conversions, MapStruct automatically applies the single-item mapping
    List<CatalogEntry> toEntityList(List<CatalogEntryDto> dtoList);
    List<CatalogEntryDto> toDtoList(List<CatalogEntry> entityList);
}