package com.pourchoices.chordpro.adapter.out.file;

import com.pourchoices.chordpro.application.domain.model.SetlistEntry;
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
    public void writeSetlistToCsv(Path outputPath, List<SetlistEntry> setlistEntries) {
        List<SetlistEntryDto> dtos = setlistEntries.stream()
                .map(e -> SetlistEntryDto.builder()
                        .set(e.getSet())
                        .songTitle(e.getTitle())
                        .songArtist(e.getArtist())
                        .key(resolveKey(e))
                        .backing(e.getBackingLabel())
                        .build())
                .toList();
        this.setlistFileWriter.writeSetlistToCsv(outputPath, dtos);
    }

    /**
     * Returns the Performance Key when one is set, falling back to the chart key.
     */
    private String resolveKey(SetlistEntry entry) {
        String pk = entry.getPerformanceKey();
        return (pk != null && !pk.isBlank()) ? pk : entry.getKey();
    }
}
