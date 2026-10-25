package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import com.pourchoices.chordpro.application.domain.model.MetadataConsistencyReport;
import com.pourchoices.chordpro.application.domain.model.MetadataConsistencyReport.FindingType;
import com.pourchoices.chordpro.application.domain.model.SongId;
import com.pourchoices.chordpro.application.port.out.CatalogPort;
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
 * Unit tests for {@link ConsistentMetadataService} — cross-variant drift
 * detection, filename/key checks, missing-performance-key inference, and
 * --fix propagation.
 */
class ConsistentMetadataServiceTest {

    private CatalogPort catalogPort;
    private ConsistentMetadataService service;

    @BeforeEach
    void setUp() {
        catalogPort = mock(CatalogPort.class);
        ChordproCatalogIndexPathConfig config = mock(ChordproCatalogIndexPathConfig.class);
        when(config.getCatalogIndexPath()).thenReturn("./song-catalog.csv");
        service = new ConsistentMetadataService(catalogPort, config, new CatalogEntryComparator());
    }

    private static CatalogEntry.CatalogEntryBuilder base(String songId, String key) {
        return CatalogEntry.builder()
                .songId(SongId.parse(songId))
                .title("Hollywood Nights")
                .artist("Bob Seger")
                .key(key)
                .duration("4:40")
                .tempo("150")
                .countin("8")
                .performanceKey("E");
    }

    /** Like {@link #base} but with no PERFORMANCE KEY set — for Check C tests. */
    private static CatalogEntry.CatalogEntryBuilder noPerfKey(String songId, String title, String key) {
        return CatalogEntry.builder()
                .songId(SongId.parse(songId))
                .title(title)
                .artist("Artist")
                .key(key)
                .duration("3:00");
    }

    private void givenCatalog(CatalogEntry... entries) {
        Map<String, CatalogEntry> catalog = new LinkedHashMap<>();
        for (CatalogEntry e : entries) {
            catalog.put(e.getSongId().toString(), e);
        }
        when(catalogPort.readCatalogFromCsv(any())).thenReturn(catalog);
    }

    // ── Check A: drift ────────────────────────────────────────────────────

    @Test
    void identicalExceptKey_isClean() {
        givenCatalog(
                base("ABC:B:BobSeger:HollywoodNights", "E").build(),
                base("ABC:B:BobSeger:HollywoodNights-b", "B").build()
        );

        MetadataConsistencyReport report = service.check(false, null);

        assertThat(report.issueCount()).isZero();
        assertThat(report.getGroupsChecked()).isEqualTo(1);
        assertThat(report.getConsistentGroups()).isEqualTo(1);
    }

    @Test
    void differentTempo_isDrift() {
        givenCatalog(
                base("ABC:B:BobSeger:HollywoodNights", "E").build(),
                base("ABC:B:BobSeger:HollywoodNights-b", "B").tempo("148").build()
        );

        MetadataConsistencyReport report = service.check(false, null);

        assertThat(report.issueCount()).isEqualTo(1);
        assertThat(report.getFindings().get(0).getType()).isEqualTo(FindingType.DRIFT);
    }

    @Test
    void differentPerformanceKey_isDrift() {
        // Performance key is the invariant — it must match across variants.
        givenCatalog(
                base("ABC:B:BobSeger:HollywoodNights", "E").build(),
                base("ABC:B:BobSeger:HollywoodNights-b", "B").performanceKey("D").build()
        );

        MetadataConsistencyReport report = service.check(false, null);

        assertThat(report.issueCount()).isEqualTo(1);
        assertThat(report.getFindings().get(0).getType()).isEqualTo(FindingType.DRIFT);
    }

    @Test
    void singleVariant_isSkipped() {
        givenCatalog(base("ABC:B:BobSeger:HollywoodNights", "E").build());

        MetadataConsistencyReport report = service.check(false, null);

        assertThat(report.getGroupsChecked()).isZero();
        assertThat(report.issueCount()).isZero();
    }

    // ── Check B: filename / key ───────────────────────────────────────────

    @Test
    void filenameKeyMismatch_isFlagged() {
        // Filename says -b (B) but {key:} is E.
        givenCatalog(
                base("ABC:B:BobSeger:HollywoodNights", "E").build(),
                base("ABC:B:BobSeger:HollywoodNights-b", "E").build()
        );

        MetadataConsistencyReport report = service.check(false, null);

        assertThat(report.getFindings())
                .anyMatch(f -> f.getType() == FindingType.FILENAME_KEY);
    }

    @Test
    void filenameKeyEnharmonic_isClean() {
        // Filename -bb (Bb) and {key:} A# are enharmonically equal.
        givenCatalog(
                base("ABC:B:BobSeger:HollywoodNights", "E").build(),
                base("ABC:B:BobSeger:HollywoodNights-bb", "A#").build()
        );

        MetadataConsistencyReport report = service.check(false, null);

        assertThat(report.getFindings())
                .noneMatch(f -> f.getType() == FindingType.FILENAME_KEY);
    }

    // ── --fix (drift) ─────────────────────────────────────────────────────

    @Test
    void fix_propagatesFromBase_andWritesCatalog() {
        givenCatalog(
                base("ABC:B:BobSeger:HollywoodNights", "E").build(),
                base("ABC:B:BobSeger:HollywoodNights-b", "B").tempo("148").build()
        );

        service.check(true, null);

        @SuppressWarnings("unchecked")
        var captor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(catalogPort).writeCatalogToCsv(any(), captor.capture());

        List<CatalogEntry> written = captor.getValue();
        CatalogEntry variant = written.stream()
                .filter(c -> c.getSongId().toString().equals("ABC:B:BobSeger:HollywoodNights-b"))
                .findFirst().orElseThrow();
        // tempo fixed from base, but key preserved
        assertThat(variant.getTempo()).isEqualTo("150");
        assertThat(variant.getKey()).isEqualTo("B");
    }

    @Test
    void dryRun_neverWritesCatalog() {
        givenCatalog(
                base("ABC:B:BobSeger:HollywoodNights", "E").build(),
                base("ABC:B:BobSeger:HollywoodNights-b", "B").tempo("148").build()
        );

        service.check(false, null);

        verify(catalogPort, never()).writeCatalogToCsv(any(), anyList());
    }

    @Test
    void fix_orphanGroupWithoutSource_isNotWritten() {
        // Two variants, no base — can't safely choose a source for --fix.
        givenCatalog(
                base("ABC:B:BobSeger:HollywoodNights-b", "B").tempo("148").build(),
                base("ABC:B:BobSeger:HollywoodNights-c", "C").build()
        );

        service.check(true, null);

        verify(catalogPort, never()).writeCatalogToCsv(any(), anyList());
    }

    // ── Check C: missing performance key, inferred from a variant's KEY ────

    @Test
    void missingPerformanceKey_oneVariant_isFlaggedInDryRun() {
        givenCatalog(
                noPerfKey("DEF:E:EltonJohn:Daniel", "Daniel", "C").build(),
                noPerfKey("DEF:E:EltonJohn:Daniel-a", "Daniel", "A").build()
        );

        MetadataConsistencyReport report = service.check(false, null);

        assertThat(report.getFindings())
                .anyMatch(f -> f.getType() == FindingType.MISSING_PERFORMANCE_KEY
                        && f.getDetails().get(0).contains("'A'"));
        // Dry-run never writes, even though this finding is auto-fixable.
        verify(catalogPort, never()).writeCatalogToCsv(any(), anyList());
    }

    @Test
    void missingPerformanceKey_oneVariant_fixSetsBothToVariantsKey() {
        givenCatalog(
                noPerfKey("DEF:E:EltonJohn:Daniel", "Daniel", "C").build(),
                noPerfKey("DEF:E:EltonJohn:Daniel-a", "Daniel", "A").build()
        );

        service.check(true, null);

        @SuppressWarnings("unchecked")
        var captor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(catalogPort).writeCatalogToCsv(any(), captor.capture());

        List<CatalogEntry> written = captor.getValue();
        assertThat(written).allMatch(e -> "A".equals(e.getPerformanceKey()));
        // KEY itself is untouched on both variants.
        CatalogEntry base = written.stream()
                .filter(e -> e.getSongId().toString().equals("DEF:E:EltonJohn:Daniel"))
                .findFirst().orElseThrow();
        assertThat(base.getKey()).isEqualTo("C");
    }

    @Test
    void missingPerformanceKey_alreadySetOnOneVariant_isNotFlagged() {
        // Not ALL variants are blank -- this is Check A's (DRIFT) territory, not Check C's.
        givenCatalog(
                noPerfKey("DEF:E:EltonJohn:Daniel", "Daniel", "C").performanceKey("C").build(),
                noPerfKey("DEF:E:EltonJohn:Daniel-a", "Daniel", "A").build()
        );

        MetadataConsistencyReport report = service.check(false, null);

        assertThat(report.getFindings())
                .noneMatch(f -> f.getType() == FindingType.MISSING_PERFORMANCE_KEY);
    }

    @Test
    void missingPerformanceKey_singleVariantGroup_isNotFlagged() {
        // No variant exists at all -- nothing to infer from.
        givenCatalog(noPerfKey("DEF:E:EltonJohn:Daniel", "Daniel", "C").build());

        MetadataConsistencyReport report = service.check(false, null);

        assertThat(report.getFindings())
                .noneMatch(f -> f.getType() == FindingType.MISSING_PERFORMANCE_KEY);
    }

    @Test
    void missingPerformanceKey_ambiguousWithMultipleVariants_isReportedNotFixed() {
        givenCatalog(
                noPerfKey("JKL:J:JudyGarland:OverTheRainbow", "Over The Rainbow", "Eb").build(),
                noPerfKey("JKL:J:JudyGarland:OverTheRainbow-bb", "Over The Rainbow", "Bb").build(),
                noPerfKey("JKL:J:JudyGarland:OverTheRainbow-e", "Over The Rainbow", "E").build()
        );

        MetadataConsistencyReport report = service.check(true, null);

        assertThat(report.getFindings())
                .anyMatch(f -> f.getType() == FindingType.MISSING_PERFORMANCE_KEY
                        && f.getDetails().get(0).contains("ambiguous"));
        verify(catalogPort, never()).writeCatalogToCsv(any(), anyList());
    }

    @Test
    void missingPerformanceKey_ambiguousButPinnedWithSource_isFixed() {
        givenCatalog(
                noPerfKey("JKL:J:JudyGarland:OverTheRainbow", "Over The Rainbow", "Eb").build(),
                noPerfKey("JKL:J:JudyGarland:OverTheRainbow-bb", "Over The Rainbow", "Bb").build(),
                noPerfKey("JKL:J:JudyGarland:OverTheRainbow-e", "Over The Rainbow", "E").build()
        );

        service.check(true, "JKL:J:JudyGarland:OverTheRainbow-e");

        @SuppressWarnings("unchecked")
        var captor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(catalogPort).writeCatalogToCsv(any(), captor.capture());
        assertThat(captor.getValue()).allMatch(e -> "E".equals(((CatalogEntry) e).getPerformanceKey()));
    }
}
