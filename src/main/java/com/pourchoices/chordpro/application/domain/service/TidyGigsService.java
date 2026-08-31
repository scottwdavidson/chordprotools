package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.GigsRowRepair;
import com.pourchoices.chordpro.application.domain.model.SetlistAssignment;
import com.pourchoices.chordpro.application.domain.model.TidyGigsResult;
import com.pourchoices.chordpro.application.port.in.TidyGigsUseCase;
import com.pourchoices.chordpro.application.port.out.SetlistAssignmentsPort;
import com.pourchoices.chordpro.config.ChordproGigsPathConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Strips carriage returns, sorts {@code gigs.csv} by GIG then SET, and -
 * before any of that - repairs the one well-understood spreadsheet-round-trip
 * defect: a row missing a trailing blank column (see {@link GigsRowRepair}).
 *
 * <p>This is deliberately the only gigs.csv reader in the app that tolerates
 * a malformed row. Every other command ({@code list-gigs}, {@code copy-gig},
 * etc.) reads strictly and fails loudly on bad data - see
 * {@link com.pourchoices.chordpro.adapter.out.file.SetlistAssignmentsFileReader#readAssignments}.
 * That's intentional: silently tolerating bad data on every read would mask
 * a still-broken file forever instead of fixing it once, here.
 */
@Service
@RequiredArgsConstructor
public class TidyGigsService implements TidyGigsUseCase {

    private final SetlistAssignmentsPort assignmentsPort;
    private final ChordproGigsPathConfig gigsPathConfig;

    @Override
    public TidyGigsResult tidyGigs() {
        Path gigsPath = Paths.get(gigsPathConfig.getGigsPath());
        List<String> rawLines = assignmentsPort.readRawLines(gigsPath);

        if (rawLines.isEmpty()) {
            return TidyGigsResult.builder().fileMissingOrEmpty(true).build();
        }

        GigsRowRepair.Report repairReport = GigsRowRepair.repair(rawLines);

        if (repairReport.hasRejections()) {
            // Refuse to touch the file - these rows need a human, not a guess.
            return TidyGigsResult.builder()
                    .rejectedRows(repairReport.getRejectedRows())
                    .build();
        }

        if (repairReport.hasRepairs()) {
            assignmentsPort.writeRawLines(gigsPath, repairReport.getRepairedLines());
        }

        List<SetlistAssignment> assignments = assignmentsPort.readAssignments(gigsPath);
        List<SetlistAssignment> sortedAssignments = assignments.stream()
                .sorted(Comparator.comparing(SetlistAssignment::getGig)
                        .thenComparing(SetlistAssignment::getSet))
                .collect(Collectors.toList());

        assignmentsPort.writeAssignments(gigsPath, sortedAssignments);

        return TidyGigsResult.builder()
                .repairedLineNumbers(repairReport.getRepairedLineNumbers())
                .tidied(true)
                .build();
    }
}
