package com.pourchoices.chordpro.adapter.out.file;

import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import com.pourchoices.chordpro.application.port.out.SetlistPort;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;

@Service
public class SetlistAdapter implements SetlistPort {

    private final SetlistFileWriter setlistFileWriter;

    public SetlistAdapter(SetlistFileWriter setlistFileWriter) {
        this.setlistFileWriter = setlistFileWriter;
    }

    @Override
    public void writeSetlistToCsv(Path outputPath, List<CatalogEntry> setlistEntries) {
        List<SetlistEntryDto> dtos = setlistEntries.stream()
                .map(e -> SetlistEntryDto.builder()
                        .set(e.getSet())
                        .songTitle(e.getTitle())
                        .songArtist(e.getArtist())
                        .key(e.getKey())
                        .build())
                .toList();
        this.setlistFileWriter.writeSetlistToCsv(outputPath, dtos);
    }
}
