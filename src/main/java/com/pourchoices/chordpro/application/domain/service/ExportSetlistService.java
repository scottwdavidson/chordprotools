package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import com.pourchoices.chordpro.application.domain.model.Setlist;
import com.pourchoices.chordpro.application.domain.model.SetlistAssignment;
import com.pourchoices.chordpro.application.domain.model.SetlistEntry;
import com.pourchoices.chordpro.application.port.in.ExportSetlistUseCase;
import com.pourchoices.chordpro.application.port.out.CatalogPort;
import com.pourchoices.chordpro.application.port.out.SetlistAssignmentsPort;
import com.pourchoices.chordpro.application.port.out.SetlistPort;
import com.pourchoices.chordpro.config.ChordproCatalogIndexPathConfig;
import com.pourchoices.chordpro.config.ChordproGigsPathConfig;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Loads the song catalog and setlist assignments, joins them for the target gig,
 * de-duplicates, sorts by set code, and writes the result to a setlist CSV.
 *
 * <p>Gig resolution: uses the explicitly supplied {@code gigParam} when non-blank;
 * otherwise defaults to the lexicographically last GIG value in
 * {@code setlist-assignments.csv} (date-first slugs sort chronologically).
 *
 * <p>De-duplication of base/variant pairs is delegated to {@link SetlistDeduplicator}.
 * The join between catalog and assignments is delegated to {@link SetlistJoiner}.
 * The CSV projection is delegated entirely to {@link SetlistPort}.
 */
@Service
@AllArgsConstructor(onConstructor_ = @__(@Autowired))
@Slf4j
public class ExportSetlistService implements ExportSetlistUseCase {

    private final CatalogPort catalogPort;
    private final SetlistPort setlistPort;
    private final SetlistAssignmentsPort assignmentsPort;
    private final ChordproCatalogIndexPathConfig catalogConfig;
    private final ChordproGigsPathConfig gigsConfig;
    private final SetlistDeduplicator deduplicator;
    private final SetlistJoiner joiner;

    @Override
    public Setlist exportSetlist(String gigParam, String outputPathString) {

        // 1. Load catalog and assignments
        Path catalogPath = Paths.get(catalogConfig.getCatalogIndexPath());
        Map<String, CatalogEntry> catalog = catalogPort.readCatalogFromCsv(catalogPath);
        log.info("Loaded {} total catalog entries from {}", catalog.size(), catalogPath);

        Path assignmentsPath = Paths.get(gigsConfig.getGigsPath());
        List<SetlistAssignment> allAssignments = assignmentsPort.readAssignments(assignmentsPath);
        log.info("Loaded {} total assignment(s)", allAssignments.size());

        // 2. Resolve gig, join catalog + assignments
        List<SetlistEntry> joined = joiner.join(gigParam, allAssignments, catalog);
        String resolvedGig = joiner.resolveGig(gigParam, allAssignments);
        log.info("Found {} entries for gig '{}' (before de-duplication)", joined.size(), resolvedGig);

        // 3. De-duplicate, then sort by set code
        List<SetlistEntry> setlistEntries = deduplicator.deduplicate(joined).stream()
                .sorted(Comparator.comparing(SetlistEntry::getSet))
                .toList();
        log.info("{} entries remain after de-duplication", setlistEntries.size());

        // 4. Wrap in the Setlist domain object
        Setlist setlist = Setlist.builder()
                .gig(resolvedGig)
                .entries(setlistEntries)
                .build();

        // 5. Write the setlist CSV
        Path outputPath = Paths.get(outputPathString);
        setlistPort.writeSetlistToCsv(outputPath, setlistEntries);
        log.info("Setlist ({} songs) written to {}", setlist.size(), outputPath.toAbsolutePath());

        return setlist;
    }
}
