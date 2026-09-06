package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.ArcThird;
import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import com.pourchoices.chordpro.application.domain.model.SetlistAssignment;
import com.pourchoices.chordpro.application.domain.model.SetlistEntry;
import com.pourchoices.chordpro.application.domain.model.SetlistEvaluationReport;
import com.pourchoices.chordpro.application.domain.model.SetlistEvaluationReport.Violation;
import com.pourchoices.chordpro.application.domain.model.SetlistEvaluationReport.ViolationType;
import com.pourchoices.chordpro.application.domain.model.VenueProfile;
import com.pourchoices.chordpro.application.port.in.EvaluateSetlistUseCase;
import com.pourchoices.chordpro.application.port.out.CatalogPort;
import com.pourchoices.chordpro.application.port.out.GigVenuePort;
import com.pourchoices.chordpro.application.port.out.SetlistAssignmentsPort;
import com.pourchoices.chordpro.application.port.out.VenueProfilePort;
import com.pourchoices.chordpro.config.ChordproCatalogIndexPathConfig;
import com.pourchoices.chordpro.config.ChordproGigVenuesPathConfig;
import com.pourchoices.chordpro.config.ChordproGigsPathConfig;
import com.pourchoices.chordpro.config.ChordproVenueProfilesPathConfig;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Joins a gig's setlist against its venue's policy and energy-arc shape.
 *
 * <p>Resolution chain: gig \u2192 ({@code gig-venues.csv}) \u2192 venue name \u2192
 * ({@code venue-profiles.csv}) \u2192 policy. Missing at either hop is not an
 * error \u2014 it just means fewer checks run, and the report says so explicitly
 * (see {@code song-characterization-and-venue-fit.md} \u00a75).
 *
 * <p>Backup (Z-set) songs are excluded before scoring, same as
 * {@code export-setlist}'s default \u2014 they're not part of the performed arc.
 */
@Service
@AllArgsConstructor(onConstructor_ = @__(@Autowired))
@Slf4j
public class EvaluateSetlistService implements EvaluateSetlistUseCase {

    private final CatalogPort catalogPort;
    private final SetlistAssignmentsPort assignmentsPort;
    private final GigVenuePort gigVenuePort;
    private final VenueProfilePort venueProfilePort;
    private final ChordproCatalogIndexPathConfig catalogConfig;
    private final ChordproGigsPathConfig gigsConfig;
    private final ChordproGigVenuesPathConfig gigVenuesConfig;
    private final ChordproVenueProfilesPathConfig venueProfilesConfig;
    private final SetlistDeduplicator deduplicator;
    private final SetlistJoiner joiner;
    private final SetlistArcScorer arcScorer;

    @Override
    public SetlistEvaluationReport evaluateSetlist(String gigParam) {

        Map<String, CatalogEntry> catalog =
                catalogPort.readCatalogFromCsv(Paths.get(catalogConfig.getCatalogIndexPath()));
        List<SetlistAssignment> allAssignments =
                assignmentsPort.readAssignments(Paths.get(gigsConfig.getGigsPath()));

        List<SetlistEntry> joined = joiner.join(gigParam, allAssignments, catalog);
        String resolvedGig = joiner.resolveGig(gigParam, allAssignments);

        List<SetlistEntry> entries = deduplicator.deduplicate(joined).stream()
                .filter(e -> !e.getSet().toUpperCase().startsWith("Z"))
                .sorted(Comparator.comparing(SetlistEntry::getSet))
                .toList();

        String venueName = resolveVenueName(resolvedGig);
        VenueProfile profile = resolveVenueProfile(venueName);

        SetlistEvaluationReport.SetlistEvaluationReportBuilder report = SetlistEvaluationReport.builder()
                .gig(resolvedGig)
                .venue(venueName)
                .venueProfile(profile)
                .totalSongs(entries.size())
                .uncharacterizedEnergyCount(countMissing(entries, e -> e.getSong().getEnergyLevel() == null))
                .uncharacterizedVocalIntensityCount(countMissing(entries, e -> e.getSong().getVocalIntensity() == null))
                .uncharacterizedSingAlongCount(countMissing(entries, e -> e.getSong().getSingAlong() == null));

        applyHardViolations(entries, venueName, profile, report);
        applyArc(entries, profile, report);
        applySingAlong(entries, report);

        return report.build();
    }

    private String resolveVenueName(String resolvedGig) {
        if (resolvedGig == null) return null;
        Map<String, String> gigToVenue =
                gigVenuePort.readGigVenues(Paths.get(gigVenuesConfig.getGigVenuesPath()));
        return gigToVenue.get(resolvedGig);
    }

    private VenueProfile resolveVenueProfile(String venueName) {
        if (venueName == null) return null;
        Map<String, VenueProfile> profiles =
                venueProfilePort.readVenueProfiles(Paths.get(venueProfilesConfig.getVenueProfilesPath()));
        return profiles.get(venueName);
    }

    private int countMissing(List<SetlistEntry> entries, java.util.function.Predicate<SetlistEntry> missing) {
        return (int) entries.stream().filter(missing).count();
    }

    private void applyHardViolations(List<SetlistEntry> entries, String venueName, VenueProfile profile,
                                      SetlistEvaluationReport.SetlistEvaluationReportBuilder report) {
        if (profile == null) {
            report.arcNote(venueName == null
                    ? "No venue linked to this gig in gig-venues.csv — skipping hard-violation checks."
                    : "Venue '" + venueName + "' has no policy configured in venue-profiles.csv — skipping hard-violation checks.");
            return;
        }
        for (SetlistEntry entry : entries) {
            for (Violation v : checkViolations(entry, profile)) {
                report.violation(v);
            }
        }
    }

    private List<Violation> checkViolations(SetlistEntry entry, VenueProfile profile) {
        CatalogEntry song = entry.getSong();
        List<Violation> found = new ArrayList<>();

        if (song.getVocalIntensity() != null && profile.getMaxVocalIntensity() != null
                && song.getVocalIntensity().ordinal() > profile.getMaxVocalIntensity().ordinal()) {
            found.add(violation(ViolationType.VOCAL_INTENSITY, entry,
                    "requires " + song.getVocalIntensity() + " vocals, venue allows at most " + profile.getMaxVocalIntensity()));
        }
        if (song.getEnergyLevel() != null && profile.getEnergyCeiling() != null
                && song.getEnergyLevel() > profile.getEnergyCeiling()) {
            found.add(violation(ViolationType.ENERGY_CEILING, entry,
                    "energy " + song.getEnergyLevel() + " exceeds venue ceiling of " + profile.getEnergyCeiling()));
        }
        if (song.getEnergyLevel() != null && profile.getEnergyFloor() != null
                && song.getEnergyLevel() < profile.getEnergyFloor()) {
            found.add(violation(ViolationType.ENERGY_FLOOR, entry,
                    "energy " + song.getEnergyLevel() + " is below venue floor of " + profile.getEnergyFloor()));
        }
        return found;
    }

    private Violation violation(ViolationType type, SetlistEntry entry, String detail) {
        return Violation.builder()
                .type(type)
                .set(entry.getSet())
                .title(entry.getTitle())
                .artist(entry.getArtist())
                .detail(detail)
                .build();
    }

    private void applyArc(List<SetlistEntry> entries, VenueProfile profile,
                           SetlistEvaluationReport.SetlistEvaluationReportBuilder report) {
        List<ArcThird> thirds = arcScorer.computeThirds(entries);
        thirds.forEach(report::arcThird);
        if (!thirds.isEmpty()) {
            report.arcNote(buildTrendNote(thirds, profile));
        }
    }

    private String buildTrendNote(List<ArcThird> thirds, VenueProfile profile) {
        ArcThird opening = thirds.get(0);
        ArcThird finalThird = thirds.get(2);

        if (opening.getAvgEnergy() == null || finalThird.getAvgEnergy() == null) {
            return "Opening/final thirds don't have enough characterized songs yet — skipping arc-trend check.";
        }

        boolean narrowRange = profile != null && profile.getEnergyCeiling() != null && profile.getEnergyFloor() != null
                && (profile.getEnergyCeiling() - profile.getEnergyFloor()) <= 3;
        if (narrowRange) {
            return String.format(
                    "Venue's energy range is narrow (floor %d, ceiling %d) — a strong arc isn't expected here.",
                    profile.getEnergyFloor(), profile.getEnergyCeiling());
        }

        double delta = finalThird.getAvgEnergy() - opening.getAvgEnergy();
        if (delta >= 0.5) {
            return String.format("Upward arc: opening avg %.1f -> final avg %.1f. Looks good.",
                    opening.getAvgEnergy(), finalThird.getAvgEnergy());
        } else if (delta <= -0.5) {
            return String.format("Downward arc: opening avg %.1f -> final avg %.1f. Consider moving more upbeat songs later.",
                    opening.getAvgEnergy(), finalThird.getAvgEnergy());
        }
        return String.format("Flat arc: opening avg %.1f, final avg %.1f. Consider building more energy toward the end.",
                opening.getAvgEnergy(), finalThird.getAvgEnergy());
    }

    private void applySingAlong(List<SetlistEntry> entries,
                                 SetlistEvaluationReport.SetlistEvaluationReportBuilder report) {
        report.singAlongCount(arcScorer.singAlongCount(entries));
        report.singAlongInFinalThird(arcScorer.singAlongCountInFinalThird(entries));
        report.lastSongsHaveSingAlong(
                arcScorer.lastSongsIncludeSingAlong(entries, SetlistArcScorer.DEFAULT_CLOSER_WINDOW));
    }
}
