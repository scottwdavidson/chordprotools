package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.SetlistAssignment;
import com.pourchoices.chordpro.application.port.in.CopyGigUseCase;
import com.pourchoices.chordpro.application.port.out.SetlistAssignmentsPort;
import com.pourchoices.chordpro.config.ChordproGigsPathConfig;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Copies all setlist assignments from one gig to a new gig slug, including
 * RC SLOT values.
 *
 * <h3>Guard-rails</h3>
 * <ul>
 *   <li>Source gig must exist in {@code gigs.csv}.</li>
 *   <li>Target gig must not already have assignments, unless {@code force = true}.</li>
 * </ul>
 */
@Service
@AllArgsConstructor(onConstructor_ = @__(@Autowired))
@Slf4j
public class CopyGigService implements CopyGigUseCase {

    private final SetlistAssignmentsPort assignmentsPort;
    private final ChordproGigsPathConfig gigsConfig;

    @Override
    public int copyGig(String sourceGig, String targetGig, boolean force) {

        // ── 1. Load assignments ──────────────────────────────────────────────
        Path assignmentsPath = Paths.get(gigsConfig.getGigsPath());
        List<SetlistAssignment> allAssignments = assignmentsPort.readAssignments(assignmentsPath);

        // ── 2. Validate source ───────────────────────────────────────────────
        List<SetlistAssignment> sourceRows = allAssignments.stream()
                .filter(a -> sourceGig.equals(a.getGig()))
                .toList();

        if (sourceRows.isEmpty()) {
            throw new IllegalArgumentException(
                    "Source gig not found in gigs.csv: '" + sourceGig + "'");
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
        // RC SLOT is copied forward: from one gig to the next, the setlist
        // typically only drops/adds a song or two, so the prior gig's slots
        // are usually still fine as-is. Run assign-backing-track-slots
        // afterward to fill in slots for any newly-added songs, or
        // --reoptimize to renumber everything from scratch.
        List<SetlistAssignment> newTargetRows = sourceRows.stream()
                .map(a -> SetlistAssignment.builder()
                        .gig(targetGig)
                        .songId(a.getSongId())
                        .set(a.getSet())
                        .rcSlot(a.getRcSlot())
                        .build())
                .toList();

        List<SetlistAssignment> retained = allAssignments.stream()
                .filter(a -> !targetGig.equals(a.getGig()))
                .toList();

        List<SetlistAssignment> merged = new ArrayList<>(retained);
        merged.addAll(newTargetRows);

        // ── 5. Write back ────────────────────────────────────────────────────
        assignmentsPort.writeAssignments(assignmentsPath, merged);
        log.info("Wrote {} total assignment(s) to {}", merged.size(), assignmentsPath);
        log.info("Copied {} song(s) from '{}' to '{}'",
                newTargetRows.size(), sourceGig, targetGig);

        return newTargetRows.size();
    }
}
