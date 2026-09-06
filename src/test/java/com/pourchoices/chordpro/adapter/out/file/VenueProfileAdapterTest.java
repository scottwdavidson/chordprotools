package com.pourchoices.chordpro.adapter.out.file;

import com.pourchoices.chordpro.application.domain.model.VenueProfile;
import com.pourchoices.chordpro.application.domain.model.VocalIntensity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link VenueProfileAdapter} (via {@link VenueProfileFileReader}) against
 * a real CSV fixture — same style as {@code SetlistAssignmentsAdapterTest}.
 */
class VenueProfileAdapterTest {

    private final VenueProfileMapper mapper = new VenueProfileMapper();
    private final VenueProfileFileReader reader = new VenueProfileFileReader(mapper);
    private final VenueProfileAdapter adapter = new VenueProfileAdapter(reader);

    @Test
    void readVenueProfiles_parsesTestFixture_keyedByGig(@TempDir Path tempDir) throws Exception {
        Path src = Path.of("src/test/resources/venue-profiles-test.csv");
        Path dest = tempDir.resolve("venue-profiles-test.csv");
        Files.copy(src, dest);

        Map<String, VenueProfile> profiles = adapter.readVenueProfiles(dest);

        assertThat(profiles).hasSize(3);

        VenueProfile restaurant = profiles.get("2026-09-06-SomeRestaurant");
        assertThat(restaurant.getMaxVocalIntensity()).isEqualTo(VocalIntensity.NONE);
        assertThat(restaurant.getEnergyCeiling()).isEqualTo(4);
        assertThat(restaurant.getEnergyFloor()).isEqualTo(1);

        VenueProfile outdoor = profiles.get("2026-09-06-Outdoor");
        assertThat(outdoor.getMaxVocalIntensity()).isEqualTo(VocalIntensity.FULL);
        assertThat(outdoor.getEnergyCeiling()).isEqualTo(10);
    }

    @Test
    void readVenueProfiles_missingGig_isAbsentNotAnError(@TempDir Path tempDir) throws Exception {
        Path src = Path.of("src/test/resources/venue-profiles-test.csv");
        Path dest = tempDir.resolve("venue-profiles-test.csv");
        Files.copy(src, dest);

        Map<String, VenueProfile> profiles = adapter.readVenueProfiles(dest);

        assertThat(profiles).doesNotContainKey("2026-09-06-SomeGigWithNoPolicyConfigured");
    }

    @Test
    void readVenueProfiles_fileDoesNotExist_returnsEmptyMap(@TempDir Path tempDir) {
        Path missing = tempDir.resolve("does-not-exist.csv");

        Map<String, VenueProfile> profiles = adapter.readVenueProfiles(missing);

        assertThat(profiles).isEmpty();
    }

    @Test
    void readVenueProfiles_headerOnlyFile_returnsEmptyMap(@TempDir Path tempDir) throws Exception {
        Path headerOnly = tempDir.resolve("venue-profiles.csv");
        Files.writeString(headerOnly, "GIG,MAX VOCAL INTENSITY,ENERGY CEILING,ENERGY FLOOR\n");

        Map<String, VenueProfile> profiles = adapter.readVenueProfiles(headerOnly);

        assertThat(profiles).isEmpty();
    }
}
