package com.pourchoices.chordpro.application.domain.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GigsRowRepairTest {

    private static final String HEADER = "GIG,SONG ID,SET,RC SLOT";

    @Test
    void wellFormedRows_noRepairsNoRejections() {
        List<String> lines = List.of(
                HEADER,
                "2026-06-27-Moods,ABC:B:BillyJoel:PianoMan,A01,",
                "2026-06-27-Moods,ABC:A:AlStewart:YearOfTheCat,A02,5"
        );

        GigsRowRepair.Report report = GigsRowRepair.repair(lines);

        assertThat(report.hasRepairs()).isFalse();
        assertThat(report.hasRejections()).isFalse();
        assertThat(report.getRepairedLines()).isEqualTo(lines);
    }

    @Test
    void rowMissingTrailingComma_isPaddedAndFlagged() {
        List<String> lines = List.of(
                HEADER,
                "2026-06-27-Moods,ABC:B:BillyJoel:PianoMan,A01,",
                "2026-06-27-Moods,STU:S:SealsCrofts:SummerBreeze,A07" // missing trailing comma
        );

        GigsRowRepair.Report report = GigsRowRepair.repair(lines);

        assertThat(report.hasRepairs()).isTrue();
        assertThat(report.getRepairedLineNumbers()).containsExactly(3);
        assertThat(report.getRepairedLines().get(2))
                .isEqualTo("2026-06-27-Moods,STU:S:SealsCrofts:SummerBreeze,A07,");
        assertThat(report.hasRejections()).isFalse();
        // Untouched rows stay byte-for-byte identical.
        assertThat(report.getRepairedLines().get(1)).isEqualTo(lines.get(1));
    }

    @Test
    void multipleShortRows_allRepairedIndependently() {
        List<String> lines = List.of(
                HEADER,
                "gig1,SONGID:A,A01",     // missing 1 field
                "gig1,SONGID:B,A02,",    // fine
                "gig1,SONGID:C,A03"      // missing 1 field
        );

        GigsRowRepair.Report report = GigsRowRepair.repair(lines);

        assertThat(report.getRepairedLineNumbers()).containsExactly(2, 4);
        assertThat(report.getRepairedLines().get(1)).isEqualTo("gig1,SONGID:A,A01,");
        assertThat(report.getRepairedLines().get(3)).isEqualTo("gig1,SONGID:C,A03,");
    }

    @Test
    void rowWithExtraField_isRejectedNotGuessed() {
        List<String> lines = List.of(
                HEADER,
                "2026-06-27-Moods,ABC:B:BillyJoel:PianoMan,A01,,extra"
        );

        GigsRowRepair.Report report = GigsRowRepair.repair(lines);

        assertThat(report.hasRejections()).isTrue();
        assertThat(report.getRejectedRows()).hasSize(1);
        assertThat(report.getRejectedRows().get(0).getLineNumber()).isEqualTo(2);
        assertThat(report.getRejectedRows().get(0).getRawContent()).isEqualTo(lines.get(1));
        // A rejected row is left completely untouched, not "half-fixed".
        assertThat(report.getRepairedLines().get(1)).isEqualTo(lines.get(1));
    }

    @Test
    void blankLines_areIgnoredEntirely() {
        List<String> lines = List.of(
                HEADER,
                "2026-06-27-Moods,ABC:B:BillyJoel:PianoMan,A01,",
                "",
                "2026-06-27-Moods,ABC:A:AlStewart:YearOfTheCat,A02,"
        );

        GigsRowRepair.Report report = GigsRowRepair.repair(lines);

        assertThat(report.hasRepairs()).isFalse();
        assertThat(report.hasRejections()).isFalse();
    }

    @Test
    void emptyInput_isANoOp() {
        GigsRowRepair.Report report = GigsRowRepair.repair(List.of());

        assertThat(report.hasRepairs()).isFalse();
        assertThat(report.hasRejections()).isFalse();
        assertThat(report.getRepairedLines()).isEmpty();
    }

    @Test
    void headerRow_isNeverInspectedOrModified() {
        // A "malformed" header would still be index 0 and must never be treated as data.
        List<String> lines = List.of(
                "GIG,SONG ID,SET,RC SLOT,EXTRA COLUMN",
                "gig1,SONGID:A,A01,,"
        );

        GigsRowRepair.Report report = GigsRowRepair.repair(lines);

        assertThat(report.hasRepairs()).isFalse();
        assertThat(report.hasRejections()).isFalse();
        assertThat(report.getRepairedLines().get(0)).isEqualTo(lines.get(0));
    }
}
