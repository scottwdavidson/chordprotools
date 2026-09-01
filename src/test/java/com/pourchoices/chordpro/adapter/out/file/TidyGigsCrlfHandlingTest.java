package com.pourchoices.chordpro.adapter.out.file;

import com.pourchoices.chordpro.application.domain.model.TidyGigsResult;
import com.pourchoices.chordpro.application.domain.service.TidyGigsService;
import com.pourchoices.chordpro.config.ChordproGigsPathConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Real (non-mocked) end-to-end check that {@code tidy-gigs} survives the
 * exact artifact an external spreadsheet tool (Excel / Numbers / Google
 * Sheets export) produces: CRLF line endings, sometimes combined with the
 * missing-trailing-column defect on the very same row.
 *
 * <p>Uses the real adapter classes (not mocks) against a {@code @TempDir}
 * file, because this is specifically a raw-bytes concern - a mocked port
 * would hide exactly the thing being verified. Lives here (not alongside
 * {@code TidyGigsServiceTest}) because {@link SetlistAssignmentsFileReader}'s
 * constructor is package-private.
 */
class TidyGigsCrlfHandlingTest {

    private static final String CRLF = "\r\n";

    @Test
    void crlfFile_isFullyNormalizedToLfWithNoStrayCarriageReturns(@TempDir Path tempDir) throws Exception {
        Path gigsPath = tempDir.resolve("gigs.csv");

        // Deliberately combine BOTH real-world defects on one file:
        // CRLF line endings throughout, AND a row missing its trailing
        // blank column (the exact shape a spreadsheet export produces).
        String rawCrlfContent = String.join(CRLF, List.of(
                "GIG,SONG ID,SET,RC SLOT",
                "gig1,ABC:B:BillyJoel:PianoMan,A01,",
                "gig1,ABC:A:AlStewart:YearOfTheCat,A02,5",
                "gig1,STU:S:SealsCrofts:SummerBreeze,A03" // missing trailing comma too
        )) + CRLF;
        Files.write(gigsPath, rawCrlfContent.getBytes(StandardCharsets.UTF_8));

        TidyGigsService service = buildRealService(gigsPath);

        TidyGigsResult result = service.tidyGigs();

        assertThat(result.isSuccessful()).isTrue();

        byte[] finalBytes = Files.readAllBytes(gigsPath);
        String finalContent = new String(finalBytes, StandardCharsets.UTF_8);

        // The headline assertion: not a single stray \r survives anywhere in the file.
        assertThat(finalContent).doesNotContain("\r");

        // And the data itself is correct, not just "CR-free" - no control
        // character silently glued onto the last field of any row.
        List<String> lines = Files.readAllLines(gigsPath);
        assertThat(lines).hasSize(4); // header + 3 rows
        for (String line : lines.subList(1, lines.size())) {
            assertThat(line.split(",", -1)).hasSize(4);
        }
        assertThat(lines).anySatisfy(line ->
                assertThat(line).isEqualTo("gig1,STU:S:SealsCrofts:SummerBreeze,A03,"));

        // Sanity: the field that DID have a real value doesn't have a
        // trailing \r baked into it (the classic silent-corruption case).
        assertThat(lines).anySatisfy(line ->
                assertThat(line).isEqualTo("gig1,ABC:A:AlStewart:YearOfTheCat,A02,5"));
    }

    private TidyGigsService buildRealService(Path gigsPath) {
        SetlistAssignmentMapper mapper = new SetlistAssignmentMapper();
        SetlistAssignmentsFileReader reader = new SetlistAssignmentsFileReader(mapper);
        SetlistAssignmentsFileWriter writer = new SetlistAssignmentsFileWriter();
        SetlistAssignmentsAdapter adapter = new SetlistAssignmentsAdapter(reader, writer, mapper);

        ChordproGigsPathConfig config = mock(ChordproGigsPathConfig.class);
        when(config.getGigsPath()).thenReturn(gigsPath.toString());

        return new TidyGigsService(adapter, config);
    }
}
