package com.pourchoices.chordpro.adapter.out.file;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link GigVenueAdapter} (via {@link GigVenueFileReader}) against a
 * real CSV fixture — same style as {@code VenueProfileAdapterTest}.
 */
class GigVenueAdapterTest {

    private final GigVenueFileReader reader = new GigVenueFileReader();
    private final GigVenueAdapter adapter = new GigVenueAdapter(reader);

    @Test
    void readGigVenues_parsesTestFixture_keyedByGig(@TempDir Path tempDir) throws Exception {
        Path src = Path.of("src/test/resources/gig-venues-test.csv");
        Path dest = tempDir.resolve("gig-venues-test.csv");
        Files.copy(src, dest);

        Map<String, String> gigToVenue = adapter.readGigVenues(dest);

        assertThat(gigToVenue).hasSize(3);
        assertThat(gigToVenue.get("2026-06-05-FF")).isEqualTo("First Friday");
        assertThat(gigToVenue.get("2026-03-27-Moods")).isEqualTo("Moods Wine Bar");
        assertThat(gigToVenue.get("2026-04-24-Moods")).isEqualTo("Moods Wine Bar");
    }

    @Test
    void readGigVenues_gigWithNoVenueYet_isAbsentNotAnError(@TempDir Path tempDir) throws Exception {
        Path src = Path.of("src/test/resources/gig-venues-test.csv");
        Path dest = tempDir.resolve("gig-venues-test.csv");
        Files.copy(src, dest);

        Map<String, String> gigToVenue = adapter.readGigVenues(dest);

        assertThat(gigToVenue).doesNotContainKey("2026-09-05-Balboa");
    }

    @Test
    void readGigVenues_duplicateGigRow_keepsFirstAndWarns(@TempDir Path tempDir) throws Exception {
        Path dest = tempDir.resolve("gig-venues-dupe.csv");
        Files.writeString(dest, """
                GIG,VENUE
                2026-06-05-FF,First Friday
                2026-06-05-FF,Some Other Venue
                """);

        Map<String, String> gigToVenue = adapter.readGigVenues(dest);

        assertThat(gigToVenue).hasSize(1);
        assertThat(gigToVenue.get("2026-06-05-FF")).isEqualTo("First Friday");
    }

    @Test
    void readGigVenues_fileDoesNotExist_returnsEmptyMap(@TempDir Path tempDir) {
        Path missing = tempDir.resolve("does-not-exist.csv");

        Map<String, String> gigToVenue = adapter.readGigVenues(missing);

        assertThat(gigToVenue).isEmpty();
    }

    @Test
    void readGigVenues_headerOnlyFile_returnsEmptyMap(@TempDir Path tempDir) throws Exception {
        Path headerOnly = tempDir.resolve("gig-venues.csv");
        Files.writeString(headerOnly, "GIG,VENUE\n");

        Map<String, String> gigToVenue = adapter.readGigVenues(headerOnly);

        assertThat(gigToVenue).isEmpty();
    }
}
