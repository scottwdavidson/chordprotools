package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import com.pourchoices.chordpro.application.domain.model.CharacterizeSongsReport;
import com.pourchoices.chordpro.application.domain.model.CharacterizeSongsReport.RatingField;
import com.pourchoices.chordpro.application.domain.model.SongId;
import com.pourchoices.chordpro.application.domain.model.SongRating;
import com.pourchoices.chordpro.application.domain.model.VocalIntensity;
import com.pourchoices.chordpro.application.port.out.CatalogPort;
import com.pourchoices.chordpro.application.port.out.SongRatingPort;
import com.pourchoices.chordpro.config.ChordproCatalogIndexPathConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CharacterizeSongsService} \u2014 the ratings-merge,
 * variant-propagation, and never-overwrite behavior.
 */
class CharacterizeSongsServiceTest {

    private CatalogPort catalogPort;
    private SongRatingPort songRatingPort;
    private CharacterizeSongsService service;

    @BeforeEach
    void setUp() {
        catalogPort = mock(CatalogPort.class);
        songRatingPort = mock(SongRatingPort.class);
        ChordproCatalogIndexPathConfig config = mock(ChordproCatalogIndexPathConfig.class);
        when(config.getCatalogIndexPath()).thenReturn("./song-catalog.csv");
        service = new CharacterizeSongsService(catalogPort, songRatingPort, config);
    }

    private CatalogEntry.CatalogEntryBuilder base(String songId, String title, String artist) {
        return CatalogEntry.builder()
                .songId(SongId.parse(songId))
                .title(title)
                .artist(artist)
                .key("C")
                .duration("3:00");
    }

    private void givenCatalog(CatalogEntry... entries) {
        Map<String, CatalogEntry> catalog = new LinkedHashMap<>();
        for (CatalogEntry e : entries) catalog.put(e.getSongId().toString(), e);
        when(catalogPort.readCatalogFromCsv(any())).thenReturn(catalog);
    }

    private void givenRatings(SongRating... ratings) {
        when(songRatingPort.readRatings(any())).thenReturn(List.of(ratings));
    }

    private SongRating rating(String groupKey, Integer energy, VocalIntensity vocal) {
        return SongRating.builder().groupKey(groupKey).energyLevel(energy).vocalIntensity(vocal).build();
    }

    @Test
    void appliesRatingToUncharacterizedSong() {
        givenCatalog(base("ABC:B:TomPetty:YouWreckMe", "You Wreck Me", "Tom Petty").build());
        givenRatings(rating("ABC:B:TomPetty:YouWreckMe", 8, VocalIntensity.FULL));

        CharacterizeSongsReport report = service.characterizeSongs("ratings.csv", true);

        assertThat(report.getTallies().get(RatingField.ENERGY_LEVEL).getApplied()).isEqualTo(1);
        assertThat(report.getTallies().get(RatingField.VOCAL_INTENSITY).getApplied()).isEqualTo(1);
        assertThat(report.getStillUnratedGroups()).isEmpty();

        @SuppressWarnings("unchecked")
        var captor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(catalogPort).writeCatalogToCsv(any(), captor.capture());
        CatalogEntry written = (CatalogEntry) captor.getValue().get(0);
        assertThat(written.getEnergyLevel()).isEqualTo(8);
        assertThat(written.getVocalIntensity()).isEqualTo(VocalIntensity.FULL);
    }

    @Test
    void neverOverwritesAlreadySetField() {
        givenCatalog(base("ABC:B:TomPetty:YouWreckMe", "You Wreck Me", "Tom Petty")
                .energyLevel(5) // already hand-set
                .build());
        givenRatings(rating("ABC:B:TomPetty:YouWreckMe", 8, VocalIntensity.FULL));

        CharacterizeSongsReport report = service.characterizeSongs("ratings.csv", true);

        assertThat(report.getTallies().get(RatingField.ENERGY_LEVEL).getApplied()).isZero();
        assertThat(report.getTallies().get(RatingField.ENERGY_LEVEL).getAlreadySetSkipped()).isEqualTo(1);
        // vocalIntensity was unset, so it should still apply independently
        assertThat(report.getTallies().get(RatingField.VOCAL_INTENSITY).getApplied()).isEqualTo(1);

        @SuppressWarnings("unchecked")
        var captor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(catalogPort).writeCatalogToCsv(any(), captor.capture());
        CatalogEntry written = (CatalogEntry) captor.getValue().get(0);
        assertThat(written.getEnergyLevel()).isEqualTo(5); // untouched
    }

    @Test
    void propagatesToAllKeyVariantsInTheGroup() {
        givenCatalog(
                base("ABC:B:TomPetty:YouWreckMe", "You Wreck Me", "Tom Petty").build(),
                base("ABC:B:TomPetty:YouWreckMe-c", "You Wreck Me", "Tom Petty").build()
        );
        givenRatings(rating("ABC:B:TomPetty:YouWreckMe", 8, VocalIntensity.FULL));

        CharacterizeSongsReport report = service.characterizeSongs("ratings.csv", true);

        assertThat(report.getTallies().get(RatingField.ENERGY_LEVEL).getApplied()).isEqualTo(2);

        @SuppressWarnings("unchecked")
        var captor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(catalogPort).writeCatalogToCsv(any(), captor.capture());
        List<CatalogEntry> written = captor.getValue();
        assertThat(written).allMatch(e -> Integer.valueOf(8).equals(e.getEnergyLevel()));
    }

    @Test
    void unmatchedRatingGroupKey_isReportedNotSilentlyDropped() {
        givenCatalog(base("ABC:B:TomPetty:YouWreckMe", "You Wreck Me", "Tom Petty").build());
        givenRatings(rating("ABC:B:Typo:DoesNotExist", 8, VocalIntensity.FULL));

        CharacterizeSongsReport report = service.characterizeSongs("ratings.csv", true);

        assertThat(report.getUnmatchedRatingGroupKeys()).containsExactly("ABC:B:Typo:DoesNotExist");
        assertThat(report.getRatingRowsMatched()).isZero();
        assertThat(report.getStillUnratedGroups()).containsExactly("You Wreck Me \u2014 Tom Petty");
    }

    @Test
    void dryRun_neverWritesCatalog() {
        givenCatalog(base("ABC:B:TomPetty:YouWreckMe", "You Wreck Me", "Tom Petty").build());
        givenRatings(rating("ABC:B:TomPetty:YouWreckMe", 8, VocalIntensity.FULL));

        service.characterizeSongs("ratings.csv", false);

        verify(catalogPort, never()).writeCatalogToCsv(any(), anyList());
    }

    @Test
    void dryRun_stillComputesFullReport() {
        givenCatalog(base("ABC:B:TomPetty:YouWreckMe", "You Wreck Me", "Tom Petty").build());
        givenRatings(rating("ABC:B:TomPetty:YouWreckMe", 8, VocalIntensity.FULL));

        CharacterizeSongsReport report = service.characterizeSongs("ratings.csv", false);

        assertThat(report.getTallies().get(RatingField.ENERGY_LEVEL).getApplied()).isEqualTo(1);
        assertThat(report.getStillUnratedGroups()).isEmpty();
    }

    @Test
    void songWithNoMatchingRatingAtAll_isStillUnrated() {
        givenCatalog(
                base("ABC:B:TomPetty:YouWreckMe", "You Wreck Me", "Tom Petty").build(),
                base("ABC:B:America:TodaysTheDay", "Today's the Day", "America").build()
        );
        givenRatings(rating("ABC:B:TomPetty:YouWreckMe", 8, VocalIntensity.FULL));

        CharacterizeSongsReport report = service.characterizeSongs("ratings.csv", true);

        assertThat(report.getTotalCatalogGroups()).isEqualTo(2);
        assertThat(report.getStillUnratedGroups()).containsExactly("Today's the Day \u2014 America");
    }
}
