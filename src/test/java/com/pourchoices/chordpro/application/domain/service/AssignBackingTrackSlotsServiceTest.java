package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.AssignBackingTrackSlotsResult;
import com.pourchoices.chordpro.application.domain.model.BackingType;
import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import com.pourchoices.chordpro.application.domain.model.ChordProPath;
import com.pourchoices.chordpro.application.domain.model.HeaderDirective;
import com.pourchoices.chordpro.application.domain.model.ParsedHeader;
import com.pourchoices.chordpro.application.domain.model.ParsedHeaderLine;
import com.pourchoices.chordpro.application.domain.model.ParsedSong;
import com.pourchoices.chordpro.application.domain.model.SetlistAssignment;
import com.pourchoices.chordpro.application.domain.model.SongId;
import com.pourchoices.chordpro.application.port.out.CatalogPort;
import com.pourchoices.chordpro.application.port.out.ChordProPort;
import com.pourchoices.chordpro.application.port.out.SetlistAssignmentsPort;
import com.pourchoices.chordpro.application.port.out.SetlistPort;
import com.pourchoices.chordpro.config.ChordproCatalogIndexPathConfig;
import com.pourchoices.chordpro.config.ChordproGigsPathConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AssignBackingTrackSlotsService} - in particular, that
 * the default (preserve) mode never touches an already-slotted RC-backed
 * song (whether the slot came from a previous algorithm run or was typed by
 * hand into gigs.csv), only fills genuinely blank rows, fills gaps before
 * extending upward, and refuses to write anything at all when gigs.csv has
 * ambiguous/conflicting data. {@code --reoptimize} is a regression check
 * against the original always-recompute behavior.
 *
 * <p>Uses the real {@link SetlistJoiner} and {@link SetlistDeduplicator}
 * (no dependencies of their own) and mocks everything at the I/O boundary.
 */
@ExtendWith(MockitoExtension.class)
class AssignBackingTrackSlotsServiceTest {

    private static final String GIG = "2026-06-27-Moods";

    @Mock CatalogPort catalogPort;
    @Mock SetlistPort setlistPort;
    @Mock SetlistAssignmentsPort assignmentsPort;
    @Mock ChordProPort chordProPort;
    @Mock SongParser songParser;
    @Mock ChordproCatalogIndexPathConfig catalogConfig;
    @Mock ChordproGigsPathConfig gigsConfig;

    private AssignBackingTrackSlotsService service;

    /** filePath -> current {meta: rc-slot} value in that "file" (null = no line at all). */
    private final Map<String, String> currentChoRcSlot = new HashMap<>();

    @BeforeEach
    void setUp() {
        lenient().when(catalogConfig.getCatalogIndexPath()).thenReturn("./song-catalog.csv");
        lenient().when(gigsConfig.getGigsPath()).thenReturn("./gigs.csv");
        lenient().when(chordProPort.read(any(Path.class))).thenReturn(List.of());
        lenient().when(songParser.parse(anyString(), anyList())).thenAnswer(invocation -> {
            String filePath = invocation.getArgument(0);
            return parsedSongWithRcSlot(filePath, currentChoRcSlot.get(filePath));
        });

        service = new AssignBackingTrackSlotsService(
                catalogPort, setlistPort, assignmentsPort, chordProPort, songParser,
                catalogConfig, gigsConfig, new SetlistDeduplicator(), new SetlistJoiner());
    }

    // ── Fixtures ─────────────────────────────────────────────────────────────

    private static CatalogEntry catalogEntry(String songId, String title, BackingType backingType) {
        return CatalogEntry.builder()
                .songId(SongId.parse(songId))
                .title(title)
                .artist("Some Artist")
                .key("G")
                .duration("3:30")
                .backingType(backingType)
                .build();
    }

    private static SetlistAssignment assignment(String songId, String set, String rcSlot) {
        return SetlistAssignment.builder()
                .gig(GIG).songId(SongId.parse(songId)).set(set).rcSlot(rcSlot).build();
    }

    private static ParsedSong parsedSongWithRcSlot(String filePath, String rcSlotValueOrNull) {
        ParsedHeader.ParsedHeaderBuilder headerBuilder =
                ParsedHeader.builder().chordProFilename(filePath);
        if (rcSlotValueOrNull != null) {
            headerBuilder.headerLine(ParsedHeaderLine.builder()
                    .headerDirective(HeaderDirective.RC_SLOT)
                    .value(rcSlotValueOrNull)
                    .build());
        }
        return ParsedSong.builder().parsedHeader(headerBuilder.build()).build();
    }

    private void givenCatalogAndAssignments(List<CatalogEntry> entries, List<SetlistAssignment> assignments) {
        Map<String, CatalogEntry> catalogMap = new HashMap<>();
        entries.forEach(e -> catalogMap.put(e.getSongId().toString(), e));
        when(catalogPort.readCatalogFromCsv(any(Path.class))).thenReturn(catalogMap);
        when(assignmentsPort.readAssignments(any(Path.class))).thenReturn(assignments);
    }

    @SuppressWarnings("unchecked")
    private List<SetlistAssignment> capturedWrittenAssignments() {
        ArgumentCaptor<List<SetlistAssignment>> captor = ArgumentCaptor.forClass(List.class);
        verify(assignmentsPort).writeAssignments(any(Path.class), captor.capture());
        return captor.getValue();
    }

    private String writtenSlotFor(String songId) {
        return capturedWrittenAssignments().stream()
                .filter(a -> songId.equals(a.getSongId().toString()))
                .findFirst().orElseThrow()
                .getRcSlot();
    }

    // ── Preserve mode: untouched existing slots ─────────────────────────────

    @Test
    void preserve_keepsExistingSlotUntouched_andSkipsChoWrite() {
        String songId = "ABC:B:BillyJoel:PianoMan";
        String filePath = ChordProPath.toFilePath(SongId.parse(songId));
        currentChoRcSlot.put(filePath, "10 (" + GIG + ")"); // already correctly synced

        givenCatalogAndAssignments(
                List.of(catalogEntry(songId, "Piano Man", BackingType.RC)),
                List.of(assignment(songId, "A01", "10")));

        AssignBackingTrackSlotsResult result = service.assignSlots(GIG, "./setlist.csv", false);

        assertThat(writtenSlotFor(songId)).isEqualTo("10");
        assertThat(result.getPreservedCount()).isEqualTo(1);
        assertThat(result.getNewlyAssignedCount()).isZero();
        verify(chordProPort, never()).write(any(), any()); // already correct — no write at all
    }

    @Test
    void preserve_handTypedSlot_isRespectedJustLikeAnAlgorithmicOne() {
        // No prior .cho annotation at all — simulates a slot typed straight
        // into gigs.csv in a spreadsheet, never yet synced.
        String songId = "ABC:B:BillyJoel:PianoMan";
        givenCatalogAndAssignments(
                List.of(catalogEntry(songId, "Piano Man", BackingType.RC)),
                List.of(assignment(songId, "A01", "23")));

        AssignBackingTrackSlotsResult result = service.assignSlots(GIG, "./setlist.csv", false);

        assertThat(writtenSlotFor(songId)).isEqualTo("23");
        assertThat(result.getPreservedCount()).isEqualTo(1);

        // But it DOES get synced into the .cho file, since it never had the annotation yet.
        ArgumentCaptor<ParsedSong> songCaptor = ArgumentCaptor.forClass(ParsedSong.class);
        verify(chordProPort).write(any(Path.class), songCaptor.capture());
        assertThat(rcSlotValue(songCaptor.getValue())).isEqualTo("23 (" + GIG + ")");
    }

    // ── Preserve mode: filling blanks ───────────────────────────────────────

    @Test
    void preserve_assignsFirstInSetSlotToNewSong() {
        String songId = "ABC:B:BillyJoel:PianoMan";
        givenCatalogAndAssignments(
                List.of(catalogEntry(songId, "Piano Man", BackingType.RC)),
                List.of(assignment(songId, "A01", null)));

        service.assignSlots(GIG, "./setlist.csv", false);

        assertThat(writtenSlotFor(songId))
                .isEqualTo(String.valueOf(AssignBackingTrackSlotsService.IN_SET_START_SLOT));
    }

    @Test
    void preserve_assignsFirstBackupSlotToNewZSetSong() {
        String songId = "ABC:B:BillyJoel:PianoMan";
        givenCatalogAndAssignments(
                List.of(catalogEntry(songId, "Piano Man", BackingType.RC)),
                List.of(assignment(songId, "Z01", null)));

        service.assignSlots(GIG, "./setlist.csv", false);

        assertThat(writtenSlotFor(songId))
                .isEqualTo(String.valueOf(AssignBackingTrackSlotsService.BACKUP_START_SLOT));
    }

    @Test
    void preserve_fillsGapBeforeExtendingUpward() {
        String kept  = "ABC:B:BillyJoel:PianoMan";
        String kept2 = "DEF:E:EltonJohn:Daniel";
        String newSong = "GHI:S:StevieWonder:Superstition";

        // Slots 5 and 7 occupied; 6 is a gap that should be filled first.
        givenCatalogAndAssignments(
                List.of(
                        catalogEntry(kept, "Piano Man", BackingType.RC),
                        catalogEntry(kept2, "Daniel", BackingType.RC),
                        catalogEntry(newSong, "Superstition", BackingType.RC)),
                List.of(
                        assignment(kept, "A01", "5"),
                        assignment(kept2, "A02", "7"),
                        assignment(newSong, "A03", null)));

        service.assignSlots(GIG, "./setlist.csv", false);

        assertThat(writtenSlotFor(newSong)).isEqualTo("6");
    }

    @Test
    void preserve_nonRcBackedSong_hasStraySlotCleared() {
        String songId = "ABC:B:BillyJoel:PianoMan";
        // Catalog says BB now, but gigs.csv still has a stale RC slot from before the change.
        givenCatalogAndAssignments(
                List.of(catalogEntry(songId, "Piano Man", BackingType.BB)),
                List.of(assignment(songId, "A01", "42")));

        service.assignSlots(GIG, "./setlist.csv", false);

        assertThat(writtenSlotFor(songId)).isNull();
    }

    // ── Preserve mode: guard-rails ───────────────────────────────────────────

    @Test
    void preserve_nonNumericSlot_throwsAndWritesNothing() {
        String songId = "ABC:B:BillyJoel:PianoMan";
        givenCatalogAndAssignments(
                List.of(catalogEntry(songId, "Piano Man", BackingType.RC)),
                List.of(assignment(songId, "A01", "abc")));

        assertThatThrownBy(() -> service.assignSlots(GIG, "./setlist.csv", false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not a number");

        verify(assignmentsPort, never()).writeAssignments(any(), any());
        verify(setlistPort, never()).writeSetlistToCsv(any(), any());
        verify(chordProPort, never()).write(any(), any());
    }

    @Test
    void preserve_outOfRangeSlot_throwsAndWritesNothing() {
        String songId = "ABC:B:BillyJoel:PianoMan";
        givenCatalogAndAssignments(
                List.of(catalogEntry(songId, "Piano Man", BackingType.RC)),
                List.of(assignment(songId, "A01", "150")));

        assertThatThrownBy(() -> service.assignSlots(GIG, "./setlist.csv", false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("outside the valid range");

        verify(assignmentsPort, never()).writeAssignments(any(), any());
    }

    @Test
    void preserve_collidingSlots_throwsAndWritesNothing() {
        String songA = "ABC:B:BillyJoel:PianoMan";
        String songB = "DEF:E:EltonJohn:Daniel";
        givenCatalogAndAssignments(
                List.of(
                        catalogEntry(songA, "Piano Man", BackingType.RC),
                        catalogEntry(songB, "Daniel", BackingType.RC)),
                List.of(
                        assignment(songA, "A01", "12"),
                        assignment(songB, "A02", "12"))); // same slot, different song

        assertThatThrownBy(() -> service.assignSlots(GIG, "./setlist.csv", false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("claimed by multiple songs");

        verify(assignmentsPort, never()).writeAssignments(any(), any());
        verify(chordProPort, never()).write(any(), any());
    }

    // ── Reoptimize mode: regression against the original algorithm ─────────

    @Test
    void reoptimize_ignoresExistingSlotsAndRecomputesFromScratch() {
        String songA = "ABC:B:BillyJoel:PianoMan"; // A-set, would keep slot 99 if preserved
        String songB = "DEF:E:EltonJohn:Daniel";    // A-set, no prior slot

        givenCatalogAndAssignments(
                List.of(
                        catalogEntry(songA, "Piano Man", BackingType.RC),
                        catalogEntry(songB, "Daniel", BackingType.RC)),
                List.of(
                        assignment(songA, "A01", "99"),
                        assignment(songB, "A02", null)));

        AssignBackingTrackSlotsResult result = service.assignSlots(GIG, "./setlist.csv", true);

        assertThat(writtenSlotFor(songA))
                .isEqualTo(String.valueOf(AssignBackingTrackSlotsService.IN_SET_START_SLOT));
        assertThat(writtenSlotFor(songB))
                .isEqualTo(String.valueOf(AssignBackingTrackSlotsService.IN_SET_START_SLOT + 1));
        assertThat(result.isReoptimized()).isTrue();
        assertThat(result.getPreservedCount()).isZero();
        assertThat(result.getNewlyAssignedCount()).isEqualTo(2);
    }

    // ── .cho sync annotation format ──────────────────────────────────────────

    @Test
    void syncsGigNameAnnotationIntoChoFile_whenSlotChanges() {
        String songId = "ABC:B:BillyJoel:PianoMan";
        String filePath = ChordProPath.toFilePath(SongId.parse(songId));
        currentChoRcSlot.put(filePath, "10 (2025-01-01-old-gig)"); // stale, different gig

        givenCatalogAndAssignments(
                List.of(catalogEntry(songId, "Piano Man", BackingType.RC)),
                List.of(assignment(songId, "A01", "10"))); // same NUMBER, different gig context

        service.assignSlots(GIG, "./setlist.csv", false);

        ArgumentCaptor<ParsedSong> songCaptor = ArgumentCaptor.forClass(ParsedSong.class);
        verify(chordProPort, times(1)).write(any(Path.class), songCaptor.capture());
        assertThat(rcSlotValue(songCaptor.getValue())).isEqualTo("10 (" + GIG + ")");
    }

    private static String rcSlotValue(ParsedSong song) {
        return song.getParsedHeader().getHeaderLines().stream()
                .filter(l -> l.getHeaderDirective() == HeaderDirective.RC_SLOT)
                .findFirst()
                .map(ParsedHeaderLine::getValue)
                .orElse(null);
    }
}
