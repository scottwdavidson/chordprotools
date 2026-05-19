package com.pourchoices.chordpro.adapter.out.file;

import com.pourchoices.chordpro.application.domain.model.SetlistAssignment;
import com.pourchoices.chordpro.application.domain.model.SongId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration-style round-trip test: write assignments to a temp file, read them
 * back, assert the result is equal to the input.
 */
class SetlistAssignmentsAdapterTest {

    private final SetlistAssignmentMapper mapper   = new SetlistAssignmentMapper();
    private final SetlistAssignmentsFileReader reader = new SetlistAssignmentsFileReader(mapper);
    private final SetlistAssignmentsFileWriter writer = new SetlistAssignmentsFileWriter();
    private final SetlistAssignmentsAdapter   adapter = new SetlistAssignmentsAdapter(reader, writer, mapper);

    @Test
    void readAssignments_parsesTestFixture(@TempDir Path tempDir) throws Exception {
        // Copy test fixture to a temp location so the test is hermetic
        Path src = Path.of("src/test/resources/setlist-assignments-test.csv");
        Path dest = tempDir.resolve("setlist-assignments-test.csv");
        java.nio.file.Files.copy(src, dest);

        List<SetlistAssignment> assignments = adapter.readAssignments(dest);

        assertThat(assignments).hasSize(4);
        assertThat(assignments.get(0).getGig()).isEqualTo("2026-06-14-rusty-nail");
        assertThat(assignments.get(0).getSongId().toString()).isEqualTo("DEF:E:EltonJohn:Daniel");
        assertThat(assignments.get(0).getSet()).isEqualTo("A01");
        // Both gigs contain Daniel — verify the second occurrence (different gig)
        assertThat(assignments.get(2).getGig()).isEqualTo("2025-10-12-theatre");
        assertThat(assignments.get(2).getSongId().toString()).isEqualTo("DEF:E:EltonJohn:Daniel");
        assertThat(assignments.get(2).getSet()).isEqualTo("A01");
    }

    @Test
    void writeAndReadBack_roundTrip(@TempDir Path tempDir) {
        List<SetlistAssignment> original = List.of(
                SetlistAssignment.builder()
                        .gig("2026-06-14-rusty-nail")
                        .songId(SongId.parse("DEF:E:EltonJohn:Daniel"))
                        .set("A01")
                        .build(),
                SetlistAssignment.builder()
                        .gig("2026-06-14-rusty-nail")
                        .songId(SongId.parse("STU:T:TravelingWilburys:HandleWithCare"))
                        .set("A02")
                        .build(),
                SetlistAssignment.builder()
                        .gig("2025-10-12-theatre")
                        .songId(SongId.parse("ABC:B:BillyJoel:MyLife"))  // base version only
                        .set("B10")
                        .build()
        );

        Path outFile = tempDir.resolve("setlist-assignments-out.csv");
        adapter.writeAssignments(outFile, original);

        List<SetlistAssignment> readBack = adapter.readAssignments(outFile);

        assertThat(readBack).hasSize(3);
        for (int i = 0; i < original.size(); i++) {
            assertThat(readBack.get(i).getGig()).isEqualTo(original.get(i).getGig());
            assertThat(readBack.get(i).getSongId()).isEqualTo(original.get(i).getSongId());
            assertThat(readBack.get(i).getSet()).isEqualTo(original.get(i).getSet());
        }
    }

    @Test
    void readAssignments_filterByGig_worksInService() throws Exception {
        // Demonstrates the service-layer filtering pattern: port returns all,
        // service filters. Both gigs in the fixture contain Daniel.
        Path src = Path.of("src/test/resources/setlist-assignments-test.csv");
        Path dest = Path.of("src/test/resources/setlist-assignments-test.csv"); // read directly

        List<SetlistAssignment> all = adapter.readAssignments(src);
        List<SetlistAssignment> rustyNailOnly = all.stream()
                .filter(a -> "2026-06-14-rusty-nail".equals(a.getGig()))
                .toList();

        assertThat(rustyNailOnly).hasSize(2);
        assertThat(rustyNailOnly).allMatch(a -> a.getGig().equals("2026-06-14-rusty-nail"));
    }
}
