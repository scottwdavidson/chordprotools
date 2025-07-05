package com.pourchoices.chordpro.adapter.out.file;

import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

// Mark this interface as a MapStruct mapper
@Mapper
public interface CatalogEntryMapper {

    // Get an instance of the generated mapper implementation
    CatalogEntryMapper INSTANCE = Mappers.getMapper(CatalogEntryMapper.class);

    // If I need a mapping :
    // @Mapping(source = "chordProFilename", target = "chordPROFileName")
    CatalogEntry toCatalogEntry(CatalogEntryDto catalogEntryDto);

    // Mapping method from CatalogEntry to CatalogEntryDto
    CatalogEntryDto toCatalogEntryDto(CatalogEntry catalogEntry);
}
