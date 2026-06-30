package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.SetlistAssignment;
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

@Service
@RequiredArgsConstructor
public class TidyGigsService implements TidyGigsUseCase {

    private final SetlistAssignmentsPort assignmentsPort;
    private final ChordproGigsPathConfig gigsPathConfig;

    @Override
    public void tidyGigs() {
        Path gigsPath = Paths.get(gigsPathConfig.getGigsPath());
        List<SetlistAssignment> assignments = assignmentsPort.readAssignments(gigsPath);

        if (assignments.isEmpty()) {
            System.out.println("gigs.csv is empty or not found. Nothing to do.");
            return;
        }

        List<SetlistAssignment> sortedAssignments = assignments.stream()
                .sorted(Comparator.comparing(SetlistAssignment::getGig)
                        .thenComparing(SetlistAssignment::getSet))
                .collect(Collectors.toList());

        assignmentsPort.writeAssignments(gigsPath, sortedAssignments);
        System.out.println("Tidied and sorted gigs.csv (by GIG and SET).");
    }
}