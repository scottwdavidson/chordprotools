package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.GigSummary;
import com.pourchoices.chordpro.application.domain.model.SetlistAssignment;
import com.pourchoices.chordpro.application.port.in.ListGigsUseCase;
import com.pourchoices.chordpro.application.port.out.SetlistAssignmentsPort;
import com.pourchoices.chordpro.config.ChordproGigsPathConfig;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Lists every distinct gig in {@code gigs.csv} with a count of the songs
 * assigned to it.
 *
 * <p>Reuses {@link SetlistAssignmentsPort} (the same reader used by every other
 * gig command) rather than parsing the CSV directly — previously this logic
 * lived in an inline-Python shell script with its own CSV parsing.
 */
@Service
@AllArgsConstructor(onConstructor_ = @__(@Autowired))
@Slf4j
public class ListGigsService implements ListGigsUseCase {

    private final SetlistAssignmentsPort assignmentsPort;
    private final ChordproGigsPathConfig gigsConfig;

    @Override
    public List<GigSummary> listGigs() {
        List<SetlistAssignment> assignments =
                assignmentsPort.readAssignments(Paths.get(gigsConfig.getGigsPath()));

        // TreeMap keeps gigs sorted by slug (date-first slugs sort chronologically).
        Map<String, Integer> counts = new TreeMap<>();
        for (SetlistAssignment a : assignments) {
            counts.merge(a.getGig(), 1, Integer::sum);
        }

        List<GigSummary> summaries = counts.entrySet().stream()
                .map(e -> new GigSummary(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(GigSummary::getGig))
                .toList();

        log.info("list-gigs → {} gig(s)", summaries.size());
        return summaries;
    }
}
