package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import com.pourchoices.chordpro.application.domain.model.Setlist;
import com.pourchoices.chordpro.application.domain.model.SetlistAssignment;
import com.pourchoices.chordpro.application.domain.model.SetlistEntry;
import com.pourchoices.chordpro.application.port.out.CatalogPort;
import com.pourchoices.chordpro.application.port.out.SetlistAssignmentsPort;
import com.pourchoices.chordpro.config.ChordproCatalogIndexPathConfig;
import com.pourchoices.chordpro.config.ChordproGigsPathConfig;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Builds the in-memory {@link Setlist} for a gig: loads the catalog and assignments,
 * joins them, de-duplicates base/variant pairs, optionally strips Z-set backup songs,
 * and sorts by set code.
 *
 * <p>Shared by every setlist-producing use case (CSV export, printable sheet) so the
 * "what is on the setlist" rules live in exactly one place.
 *
 * <p>Gig resolution: uses the explicitly supplied {@code gigParam} when non-blank;
 * otherwise defaults to the lexicographically last GIG value in {@code gigs.csv}.
 */
@Component
@AllArgsConstructor(onConstructor_ = @__(@Autowired))
@Slf4j
public class SetlistAssembler {

    private final CatalogPort catalogPort;
    private final SetlistAssignmentsPort assignmentsPort;
    private final ChordproCatalogIndexPathConfig catalogConfig;
    private final ChordproGigsPathConfig gigsConfig;
    private final SetlistDeduplicator deduplicator;
    private final SetlistJoiner joiner;

    /**
     * @param gigParam      gig slug, or {@code null}/blank to use the latest gig
     * @param includeBackup when {@code true}, Z-set backup songs are kept
     */
    public Setlist assemble(String gigParam, boolean includeBackup) {

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

        // 3. De-duplicate, optionally strip backup (Z-set) songs, sort by set code
        List<SetlistEntry> entries = deduplicator.deduplicate(joined).stream()
                .filter(e -> includeBackup || !e.getSet().toUpperCase().startsWith("Z"))
                .sorted(Comparator.comparing(SetlistEntry::getSet))
                .toList();
        log.info("{} entries after de-duplication{}",
                entries.size(), includeBackup ? " (backup included)" : " (backup excluded)");

        return Setlist.builder().gig(resolvedGig).entries(entries).build();
    }
}
