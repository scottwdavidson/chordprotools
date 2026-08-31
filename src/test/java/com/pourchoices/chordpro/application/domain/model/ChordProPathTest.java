package com.pourchoices.chordpro.application.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ChordProPath}. Had zero direct coverage before —
 * every other test exercised it only indirectly through a service.
 */
class ChordProPathTest {

    @Test
    void toFilePath_buildsRelativePathFromSongId() {
        SongId songId = SongId.parse("ABC:B:BillyJoel:PianoMan");

        assertThat(ChordProPath.toFilePath(songId)).isEqualTo("./cho/ABC/B/BillyJoel/PianoMan.cho");
    }

    @Test
    void toFilePath_includesKeyAlternativeSuffix() {
        SongId songId = SongId.parse("ABC:B:BillyJoel:PianoMan-a");

        assertThat(ChordProPath.toFilePath(songId)).isEqualTo("./cho/ABC/B/BillyJoel/PianoMan-a.cho");
    }

    @Test
    void toSongId_parsesRelativePathWithoutDotSlash() {
        SongId songId = ChordProPath.toSongId("cho/ABC/B/BillyJoel/PianoMan.cho");

        assertThat(songId.toString()).isEqualTo("ABC:B:BillyJoel:PianoMan");
    }

    @Test
    void toSongId_parsesRelativePathWithDotSlash() {
        SongId songId = ChordProPath.toSongId("./cho/ABC/B/BillyJoel/PianoMan.cho");

        assertThat(songId.toString()).isEqualTo("ABC:B:BillyJoel:PianoMan");
    }

    @Test
    void toSongId_roundTripsWithToFilePath() {
        SongId original = SongId.parse("ABC:B:BillyJoel:PianoMan-a");

        SongId roundTripped = ChordProPath.toSongId(ChordProPath.toFilePath(original));

        assertThat(roundTripped).isEqualTo(original);
    }

    /**
     * See the session notes: an absolute path (exactly what JUnit's
     * {@code @TempDir} always hands back) silently mis-parses without this
     * guard — producing a confusing "got N segments" error deep inside
     * {@link SongId#parse} instead of a clear, actionable one here.
     */
    @Test
    void toSongId_absolutePath_throwsClearError() {
        String absolutePath = "/Users/someone/chordprotools/cho/ABC/B/BillyJoel/PianoMan.cho";

        assertThatThrownBy(() -> ChordProPath.toSongId(absolutePath))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Expected a path starting with \"cho/\"")
                .hasMessageContaining(absolutePath);
    }

    @Test
    void toSongId_pathOutsideChoTree_throwsClearError() {
        assertThatThrownBy(() -> ChordProPath.toSongId("/tmp/some-random-file.cho"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Expected a path starting with \"cho/\"");
    }
}
