package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.port.in.TidyGigsUseCase;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TidyGigsService implements TidyGigsUseCase {

    @Override
    public void tidyGigs() {
        Path path = Paths.get("gigs.csv");
        if (!Files.exists(path)) {
            System.err.println("gigs.csv not found.");
            return;
        }

        try {
            List<String> lines = Files.readAllLines(path);
            if (lines.isEmpty()) return;

            // Keep the header row as-is (just clean out \r)
            String header = lines.get(0).replace("\r", "");

            // Parse, clean, sort, and gather the entries
            List<GigEntry> entries = lines.stream()
                    .skip(1)
                    .filter(line -> !line.trim().isEmpty())
                    .map(GigEntry::new)
                    .sorted(Comparator.comparing(GigEntry::getGig).thenComparing(GigEntry::getSet))
                    .collect(Collectors.toList());

            // Reconstruct the file contents
            List<String> outputLines = new ArrayList<>();
            outputLines.add(header);
            entries.forEach(entry -> outputLines.add(entry.getCleanedLine()));

            Files.write(path, outputLines);
            System.out.println("Tidied and sorted gigs.csv (by GIG and SET).");
        } catch (Exception e) {
            throw new RuntimeException("Failed to process gigs.csv", e);
        }
    }

    /**
     * Helper class to hold parsed rows for sorting safely.
     */
    private static class GigEntry {
        private final String cleanedLine;
        private final String gig;
        private final String set;

        public GigEntry(String rawLine) {
            this.cleanedLine = rawLine.replace("\r", "");
            String[] parts = this.cleanedLine.split(",", -1);
            
            // gigs.csv layout -> GIG (0), SONG ID (1), SET (2), RC SLOT (3)
            this.gig = parts.length > 0 ? parts[0].trim() : "";
            this.set = parts.length > 2 ? parts[2].trim() : "";
        }

        public String getCleanedLine() { return cleanedLine; }
        public String getGig() { return gig; }
        public String getSet() { return set; }
    }
}