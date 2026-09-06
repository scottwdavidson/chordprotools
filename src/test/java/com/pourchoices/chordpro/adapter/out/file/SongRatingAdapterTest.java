package com.pourchoices.chordpro.adapter.out.file;

import com.pourchoices.chordpro.application.domain.model.SongRating;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link SongRatingAdapter} (via {@link SongRatingFileReader}) against a real CSV fixture.
 */
class SongRatingAdapterTest {

    private final SongRatingMapper mapper = new SongRatingMapper();
    private final SongRatingFileReader reader = new SongRatingFileReader(mapper);
    private final SongRatingAdapter adapter = new SongRatingAdapter(reader);

    @Test
    void readRatings_parsesTestFixture(@TempDir Path tempDir) throws Exception {
        Path src = Path.of("src/test/resources/song-ratings-test.csv");
        Path dest = tempDir.resolve("song-ratings-test.csv");
        Files.copy(src, dest);

        List<SongRating> ratings = adapter.readRatings(dest);

        assertThat(ratings).hasSize(2);
        assertThat(ratings.get(0).getGroupKey()).isEqualTo("ABC:B:TomPetty:YouWreckMe");
        assertThat(ratings.get(0).getEnergyLevel()).isEqualTo(8);
    }

    @Test
    void readRatings_fileDoesNotExist_returnsEmptyList(@TempDir Path tempDir) {
        Path missing = tempDir.resolve("does-not-exist.csv");

        assertThat(adapter.readRatings(missing)).isEmpty();
    }
}
