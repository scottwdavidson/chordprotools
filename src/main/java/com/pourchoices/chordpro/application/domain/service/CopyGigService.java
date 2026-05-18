package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import com.pourchoices.chordpro.application.domain.model.SetlistAssignment;
import com.pourchoices.chordpro.application.port.in.CopyGigUseCase;
import com.pourchoices.chordpro.application.port.out.CatalogPort;
import com.pourchoices.chordpro.application.port.out.SetlistAssignmentsPort;
import com.pourchoices.chordpro.config.ChordproCatalogIndexPathConfig;
import com.pourchoices.chordpro.config.ChordproSetlistAssignmentsPathConfig;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Copies all setlist assignments from one gig to a new gig slug.
 *
 * <p>The full assignments file is rewritten with enriched TITLE and ARTIST
 * columns so it is human-readable when opened in Google Sheets or Excel.
 *
 * <h3>Guard-rails</h3>
 * <ul>
 *   <li>Source gig must exist in {@code setlist-assignments.csv}.</li>
 *   <li>Target gig must not already have assignments, unless {@code force = true}.</li>
 * </ul>
 */
@Service
@AllArgsConstructor(onConstructor_ = @__(@Autowired))
@Slf4j
public class CopyGigService implements CopyGigUseCase {

    private final SetlistAssignmentsPort assignmentsPort;
    private final CatalogPort catalogPort;
    private final ChordproSetlistAssignmentsPathConfig assignmentsConfig;
    private final ChordproCatalogIndexPathConfig catalogConfig;

    @Override
    public int copyGig(String sourceGig, String targetGig, boolean force) {

        // ── 1. Load everything ───────────────────────────────────────────────
        Path assignmentsPath = Paths.get(assignmentsConfig.getSetlistAssignmentsPath());
        List<SetlistAssignment> allAssignments = assignmentsPort.readAssignments(assignmentsPath);

        Path catalogPath = Paths.get(catalogConfig.getCatalogIndexPath());
        Map<String, CatalogEntry> catalog = catalogPort.readCatalogFromCsv(catalogPath);

        // ── 2. Validate source ───────────────────────────────────────────────
        List<SetlistAssignment> sourceRows = allAssignments.stream()
                .filter(a -> sourceGig.equals(a.getGig()))
                .toList();

        if (sourceRows.isEmpty()) {
            throw new IllegalArgumentException(
                    "Source gig not found in setlist-assignments.csv: '" + sourceGig + "'");
        }
        log.info("Found {} assignment(s) in source gig '{}'", sourceRows.size(), sourceGig);

        // ── 3. Validate target ───────────────────────────────────────────────
        Set<String> existingGigs = allAssignments.stream()
                .map(SetlistAssignment::getGig)
                .collect(Collectors.toSet());

        if (existingGigs.contains(targetGig) && !force) {
            throw new IllegalArgumentException(
                    "Target gig '" + targetGig + "' already has assignments. "
                    + "Use --force to replace them.");
        }
        if (existingGigs.contains(targetGig)) {
            log.warn("--force: replacing existing assignments for gig '{}'", targetGig);
        }

        // ── 4. Build the new full assignments list ───────────────────────────
        List<SetlistAssignment> newTargetRows = sourceRows.stream()
                .map(a -> SetlistAssignment.builder()
                        .gig(targetGig)
                        .songId(a.getSongId())
                        .set(a.getSet())
                        .build())
                .toList();

        List<SetlistAssignment> retained = allAssignments.stream()
                .filter(a -> !targetGig.equals(a.getGig()))
                .toList();

        List<SetlistAssignment> merged = new ArrayList<>(retained);
        merged.addAll(newTargetRows);

        // ── 5. Write back enriched (with TITLE and ARTIST columns) ───────────
        assignmentsPort.writeEnrichedAssignments(assignmentsPath, merged, catalog);
        log.info("Wrote {} total assignment(s) to {}", merged.size(), assignmentsPath);
        log.info("Copied {} song(s) from '{}' to '{}'",
                newTargetRows.size(), sourceGig, targetGig);

        return newTargetRows.size();
    }
}
