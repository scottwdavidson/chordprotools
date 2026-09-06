package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import com.pourchoices.chordpro.application.domain.model.SetlistAssignment;
import com.pourchoices.chordpro.application.domain.model.SetlistEvaluationReport;
import com.pourchoices.chordpro.application.domain.model.SongId;
import com.pourchoices.chordpro.application.domain.model.VenueProfile;
import com.pourchoices.chordpro.application.domain.model.VocalIntensity;
import com.pourchoices.chordpro.application.port.out.CatalogPort;
import com.pourchoices.chordpro.application.port.out.GigVenuePort;
import com.pourchoices.chordpro.application.port.out.SetlistAssignmentsPort;
import com.pourchoices.chordpro.application.port.out.VenueProfilePort;
import com.pourchoices.chordpro.config.ChordproCatalogIndexPathConfig;
import com.pourchoices.chordpro.config.ChordproGigVenuesPathConfig;
import com.pourchoices.chordpro.config.ChordproGigsPathConfig;
import com.pourchoices.chordpro.config.ChordproVenueProfilesPathConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link EvaluateSetlistService} — the gig -> venue ->
 * policy resolution chain and graceful degradation when data is missing.
 */
class EvaluateSetlistServiceTest {

    private static final String GIG = "2026-06-05-FF";

    private CatalogPort catalogPort;
    private SetlistAssignmentsPort assignmentsPort;
    private GigVenuePort gigVenuePort;
    private VenueProfilePort venueProfilePort;
    private EvaluateSetlistService service;

    @BeforeEach
    void setUp() {
        catalogPort = mock(CatalogPort.class);
        assignmentsPort = mock(SetlistAssignmentsPort.class);
        gigVenuePort = mock(GigVenuePort.class);
        venueProfilePort = mock(VenueProfilePort.class);

        ChordproCatalogIndexPathConfig catalogConfig = mock(ChordproCatalogIndexPathConfig.class);
        when(catalogConfig.getCatalogIndexPath()).thenReturn("./song-catalog.csv");
        ChordproGigsPathConfig gigsConfig = mock(ChordproGigsPathConfig.class);
        when(gigsConfig.getGigsPath()).thenReturn("./gigs.csv");
        ChordproGigVenuesPathConfig gigVenuesConfig = mock(ChordproGigVenuesPathConfig.class);
        when(gigVenuesConfig.getGigVenuesPath()).thenReturn("./gig-venues.csv");
        ChordproVenueProfilesPathConfig venueProfilesConfig = mock(ChordproVenueProfilesPathConfig.class);
        when(venueProfilesConfig.getVenueProfilesPath()).thenReturn("./venue-profiles.csv");

        service = new EvaluateSetlistService(
                catalogPort, assignmentsPort, gigVenuePort, venueProfilePort,
                catalogConfig, gigsConfig, gigVenuesConfig, venueProfilesConfig,
                new SetlistDeduplicator(), new SetlistJoiner(), new SetlistArcScorer());
    }

    private CatalogEntry song(String id, Integer energyLevel, VocalIntensity vocal, Boolean singAlong) {
        return CatalogEntry.builder()
                .songId(SongId.parse(id))
                .title(id)
                .artist("Artist")
                .key("C")
                .duration("3:00")
                .energyLevel(energyLevel)
                .vocalIntensity(vocal)
                .singAlong(singAlong)
                .build();
    }

    private void givenCatalog(CatalogEntry... entries) {
        Map<String, CatalogEntry> catalog = new LinkedHashMap<>();
        for (CatalogEntry e : entries) catalog.put(e.getSongId().toString(), e);
        when(catalogPort.readCatalogFromCsv(any())).thenReturn(catalog);
    }

    private void givenAssignments(SetlistAssignment... assignments) {
        when(assignmentsPort.readAssignments(any())).thenReturn(List.of(assignments));
    }

    private SetlistAssignment assign(String set, String songId) {
        return SetlistAssignment.builder().gig(GIG).songId(SongId.parse(songId)).set(set).build();
    }

    @Test
    void noVenueLinked_skipsHardViolationChecks_butStillComputesArc() {
        givenCatalog(song("ABC:B:Artist:Song1", 5, null, null));
        givenAssignments(assign("A01", "ABC:B:Artist:Song1"));
        when(gigVenuePort.readGigVenues(any())).thenReturn(Map.of());

        SetlistEvaluationReport report = service.evaluateSetlist(GIG);

        assertThat(report.getVenue()).isNull();
        assertThat(report.getVenueProfile()).isNull();
        assertThat(report.violationCount()).isZero();
        assertThat(report.getArcNotes()).anyMatch(n -> n.contains("No venue linked"));
        assertThat(report.getTotalSongs()).isEqualTo(1);
    }

    @Test
    void venueLinkedButNoProfileConfigured_skipsHardViolationChecks() {
        givenCatalog(song("ABC:B:Artist:Song1", 5, null, null));
        givenAssignments(assign("A01", "ABC:B:Artist:Song1"));
        when(gigVenuePort.readGigVenues(any())).thenReturn(Map.of(GIG, "Some New Venue"));
        when(venueProfilePort.readVenueProfiles(any())).thenReturn(Map.of());

        SetlistEvaluationReport report = service.evaluateSetlist(GIG);

        assertThat(report.getVenue()).isEqualTo("Some New Venue");
        assertThat(report.getVenueProfile()).isNull();
        assertThat(report.getArcNotes()).anyMatch(n -> n.contains("has no policy configured"));
    }

    @Test
    void vocalIntensityExceedsCeiling_isAViolation() {
        givenCatalog(song("ABC:B:Artist:Song1", 5, VocalIntensity.FULL, null));
        givenAssignments(assign("A01", "ABC:B:Artist:Song1"));
        when(gigVenuePort.readGigVenues(any())).thenReturn(Map.of(GIG, "Blue Vase"));
        when(venueProfilePort.readVenueProfiles(any())).thenReturn(Map.of(
                "Blue Vase", VenueProfile.builder().venue("Blue Vase").maxVocalIntensity(VocalIntensity.NONE).build()));

        SetlistEvaluationReport report = service.evaluateSetlist(GIG);

        assertThat(report.violationCount()).isEqualTo(1);
        assertThat(report.getViolations().get(0).getType())
                .isEqualTo(SetlistEvaluationReport.ViolationType.VOCAL_INTENSITY);
    }

    @Test
    void energyAboveCeiling_isAViolation() {
        givenCatalog(song("ABC:B:Artist:Song1", 9, null, null));
        givenAssignments(assign("A01", "ABC:B:Artist:Song1"));
        when(gigVenuePort.readGigVenues(any())).thenReturn(Map.of(GIG, "Moods Wine Bar"));
        when(venueProfilePort.readVenueProfiles(any())).thenReturn(Map.of(
                "Moods Wine Bar", VenueProfile.builder().venue("Moods Wine Bar").energyCeiling(8).build()));

        SetlistEvaluationReport report = service.evaluateSetlist(GIG);

        assertThat(report.violationCount()).isEqualTo(1);
        assertThat(report.getViolations().get(0).getType())
                .isEqualTo(SetlistEvaluationReport.ViolationType.ENERGY_CEILING);
    }

    @Test
    void energyBelowFloor_isAViolation() {
        givenCatalog(song("ABC:B:Artist:Song1", 1, null, null));
        givenAssignments(assign("A01", "ABC:B:Artist:Song1"));
        when(gigVenuePort.readGigVenues(any())).thenReturn(Map.of(GIG, "First Friday"));
        when(venueProfilePort.readVenueProfiles(any())).thenReturn(Map.of(
                "First Friday", VenueProfile.builder().venue("First Friday").energyFloor(3).build()));

        SetlistEvaluationReport report = service.evaluateSetlist(GIG);

        assertThat(report.violationCount()).isEqualTo(1);
        assertThat(report.getViolations().get(0).getType())
                .isEqualTo(SetlistEvaluationReport.ViolationType.ENERGY_FLOOR);
    }

    @Test
    void songWithinPolicy_isNotAViolation() {
        givenCatalog(song("ABC:B:Artist:Song1", 5, VocalIntensity.LIGHT, null));
        givenAssignments(assign("A01", "ABC:B:Artist:Song1"));
        when(gigVenuePort.readGigVenues(any())).thenReturn(Map.of(GIG, "DeRose Winery"));
        when(venueProfilePort.readVenueProfiles(any())).thenReturn(Map.of(
                "DeRose Winery", VenueProfile.builder().venue("DeRose Winery")
                        .maxVocalIntensity(VocalIntensity.LIGHT).energyCeiling(6).energyFloor(1).build()));

        SetlistEvaluationReport report = service.evaluateSetlist(GIG);

        assertThat(report.violationCount()).isZero();
    }

    @Test
    void nullEnergyOrVocal_isNeverAViolation_justCountedAsUncharacterized() {
        givenCatalog(song("ABC:B:Artist:Song1", null, null, null));
        givenAssignments(assign("A01", "ABC:B:Artist:Song1"));
        when(gigVenuePort.readGigVenues(any())).thenReturn(Map.of(GIG, "Blue Vase"));
        when(venueProfilePort.readVenueProfiles(any())).thenReturn(Map.of(
                "Blue Vase", VenueProfile.builder().venue("Blue Vase").maxVocalIntensity(VocalIntensity.NONE)
                        .energyCeiling(3).energyFloor(1).build()));

        SetlistEvaluationReport report = service.evaluateSetlist(GIG);

        assertThat(report.violationCount()).isZero();
        assertThat(report.getUncharacterizedEnergyCount()).isEqualTo(1);
        assertThat(report.getUncharacterizedVocalIntensityCount()).isEqualTo(1);
    }

    @Test
    void zSetBackupSongs_areExcludedFromEvaluation() {
        givenCatalog(
                song("ABC:B:Artist:Song1", 5, null, null),
                song("ABC:B:Artist:Song2", 9, null, null));
        givenAssignments(assign("A01", "ABC:B:Artist:Song1"), assign("Z1", "ABC:B:Artist:Song2"));
        when(gigVenuePort.readGigVenues(any())).thenReturn(Map.of(GIG, "Moods Wine Bar"));
        when(venueProfilePort.readVenueProfiles(any())).thenReturn(Map.of(
                "Moods Wine Bar", VenueProfile.builder().venue("Moods Wine Bar").energyCeiling(8).build()));

        SetlistEvaluationReport report = service.evaluateSetlist(GIG);

        // Song2 (energy 9, over the ceiling of 8) is Z-set backup -- excluded entirely.
        assertThat(report.getTotalSongs()).isEqualTo(1);
        assertThat(report.violationCount()).isZero();
    }
}
