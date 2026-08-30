package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import com.pourchoices.chordpro.application.domain.model.SemanticDiffReport;
import com.pourchoices.chordpro.application.domain.model.SongId;
import com.pourchoices.chordpro.application.port.in.VerifySyncUseCase;
import com.pourchoices.chordpro.application.port.out.CatalogPort;
import com.pourchoices.chordpro.config.ChordproCatalogIndexPathConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ConsistentSongDataService}. Mocks {@link CatalogPort}
 * and the {@link VerifySyncUseCase} <em>port</em> directly (not the concrete
 * {@link SemanticDiffService}) — this service only needs to know it's
 * calling the drift engine's contract, not its implementation.
 */
@ExtendWith(MockitoExtension.class)
class ConsistentSongDataServiceTest {

    @Mock CatalogPort catalogPort;
    @Mock ChordproCatalogIndexPathConfig catalogConfig;
    @Mock VerifySyncUseCase verifySyncUseCase;

    private ConsistentSongDataService service;

    private static CatalogEntry entry(String songId, String key) {
        return CatalogEntry.builder()
                .songId(SongId.parse(songId))
                .title("Hollywood Nights")
                .artist("Bob Seger")
                .key(key)
                .duration("5:15")
                .build();
    }

    private static final CatalogEntry BASE   = entry("ABC:B:BobSeger:HollywoodNights", "E");
    private static final CatalogEntry VARIANT_B = entry("ABC:B:BobSeger:HollywoodNights-b", "Bb");
    private static final CatalogEntry VARIANT_D = entry("ABC:B:BobSeger:HollywoodNights-d", "D");
    private static final CatalogEntry OTHER_SONG = entry("DEF:E:EltonJohn:Daniel", "C");

    @BeforeEach
    void setUp() {
        when(catalogConfig.getCatalogIndexPath()).thenReturn("./song-catalog.csv");
        service = new ConsistentSongDataService(catalogPort, catalogConfig, verifySyncUseCase);
    }

    private void stubCatalog(CatalogEntry... entries) {
        Map<String, CatalogEntry> catalog = new LinkedHashMap<>();
        for (CatalogEntry e : entries) {
            catalog.put(e.getSongId().toString(), e);
        }
        when(catalogPort.readCatalogFromCsv(any(Path.class))).thenReturn(catalog);
    }

    @Test
    void check_twoVariants_comparesBaseAgainstVariant() {
        stubCatalog(BASE, VARIANT_B, OTHER_SONG);
        when(verifySyncUseCase.verifySync(anyString(), anyString())).thenReturn(
                SemanticDiffReport.builder().fileA("a").fileB("b").build());

        List<SemanticDiffReport> reports = service.check(SongId.parse("ABC:B:BobSeger:HollywoodNights"));

        assertThat(reports).hasSize(1);
        verify(verifySyncUseCase).verifySync(
                "./cho/ABC/B/BobSeger/HollywoodNights.cho",
                "./cho/ABC/B/BobSeger/HollywoodNights-b.cho");
    }

    @Test
    void check_threeVariants_comparesBaseAgainstEachOtherVariant() {
        stubCatalog(BASE, VARIANT_B, VARIANT_D);
        when(verifySyncUseCase.verifySync(anyString(), anyString())).thenReturn(
                SemanticDiffReport.builder().fileA("a").fileB("b").build());

        List<SemanticDiffReport> reports = service.check(SongId.parse("ABC:B:BobSeger:HollywoodNights"));

        assertThat(reports).hasSize(2);
        verify(verifySyncUseCase).verifySync(
                "./cho/ABC/B/BobSeger/HollywoodNights.cho",
                "./cho/ABC/B/BobSeger/HollywoodNights-b.cho");
        verify(verifySyncUseCase).verifySync(
                "./cho/ABC/B/BobSeger/HollywoodNights.cho",
                "./cho/ABC/B/BobSeger/HollywoodNights-d.cho");
    }

    @Test
    void check_canBeInvokedFromAnyVariantsSongId_sameGroupResult() {
        stubCatalog(BASE, VARIANT_B);
        when(verifySyncUseCase.verifySync(anyString(), anyString())).thenReturn(
                SemanticDiffReport.builder().fileA("a").fileB("b").build());

        // Asking about the variant should check the whole group, same as the base.
        List<SemanticDiffReport> reports = service.check(SongId.parse("ABC:B:BobSeger:HollywoodNights-b"));

        assertThat(reports).hasSize(1);
    }

    @Test
    void check_onlyOneVariant_returnsEmptyListWithoutCallingVerifySync() {
        stubCatalog(BASE, OTHER_SONG);

        List<SemanticDiffReport> reports = service.check(SongId.parse("ABC:B:BobSeger:HollywoodNights"));

        assertThat(reports).isEmpty();
    }

    @Test
    void check_noEntriesFound_throws() {
        stubCatalog(OTHER_SONG);

        assertThatThrownBy(() -> service.check(SongId.parse("ABC:B:BobSeger:HollywoodNights")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No catalog entries found");
    }

    @Test
    void check_orphanGroupWithNoBaseVariant_usesFirstEntryAsReferenceWithoutCrashing() {
        // Neither variant is the base (standard-key) version.
        stubCatalog(VARIANT_B, VARIANT_D);
        when(verifySyncUseCase.verifySync(anyString(), anyString())).thenReturn(
                SemanticDiffReport.builder().fileA("a").fileB("b").build());

        List<SemanticDiffReport> reports = service.check(SongId.parse("ABC:B:BobSeger:HollywoodNights-b"));

        assertThat(reports).hasSize(1);
    }

    @Test
    void check_aggregatesIssueCountsAcrossVariants() {
        stubCatalog(BASE, VARIANT_B, VARIANT_D);
        when(verifySyncUseCase.verifySync(
                "./cho/ABC/B/BobSeger/HollywoodNights.cho", "./cho/ABC/B/BobSeger/HollywoodNights-b.cho"))
                .thenReturn(SemanticDiffReport.builder().fileA("a").fileB("b")
                        .finding(SemanticDiffReport.Finding.builder()
                                .type(SemanticDiffReport.FindingType.LYRIC_DESYNC)
                                .lineNumber(1).detail("drift").build())
                        .build());
        when(verifySyncUseCase.verifySync(
                "./cho/ABC/B/BobSeger/HollywoodNights.cho", "./cho/ABC/B/BobSeger/HollywoodNights-d.cho"))
                .thenReturn(SemanticDiffReport.builder().fileA("a").fileB("c").build());

        List<SemanticDiffReport> reports = service.check(SongId.parse("ABC:B:BobSeger:HollywoodNights"));

        int totalIssues = reports.stream().mapToInt(SemanticDiffReport::issueCount).sum();
        assertThat(totalIssues).isEqualTo(1);
    }
}
