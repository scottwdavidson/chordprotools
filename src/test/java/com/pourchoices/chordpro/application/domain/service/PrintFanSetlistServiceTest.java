package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import com.pourchoices.chordpro.application.domain.model.Setlist;
import com.pourchoices.chordpro.application.domain.model.SetlistAssignment;
import com.pourchoices.chordpro.application.domain.model.SetlistEntry;
import com.pourchoices.chordpro.application.domain.model.SongId;
import com.pourchoices.chordpro.application.port.out.FanSetlistSheetPort;
import com.pourchoices.chordpro.application.port.out.GigVenuePort;
import com.pourchoices.chordpro.config.ChordproGigVenuesPathConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Unit tests for {@link PrintFanSetlistService}: wiring of assembler, splitter, venue lookup and port. */
class PrintFanSetlistServiceTest {

    private static final String GIG = "2026-11-20-MOODS";

    private SetlistAssembler assembler;
    private GigVenuePort gigVenuePort;
    private FanSetlistSheetPort sheetPort;
    private PrintFanSetlistService service;

    @BeforeEach
    void setUp() {
        assembler = mock(SetlistAssembler.class);
        gigVenuePort = mock(GigVenuePort.class);
        sheetPort = mock(FanSetlistSheetPort.class);
        ChordproGigVenuesPathConfig config = mock(ChordproGigVenuesPathConfig.class);
        when(config.getGigVenuesPath()).thenReturn("./gig-venues.csv");
        service = new PrintFanSetlistService(assembler, new SetlistSetSplitter(), gigVenuePort, config, sheetPort);
    }

    private SetlistEntry entry(String set, String title) {
        CatalogEntry song = CatalogEntry.builder()
                .songId(SongId.parse("ABC:B:TestArtist:" + title)).title(title).artist("Them")
                .key("C").duration("3:00").build();
        SetlistAssignment assignment = SetlistAssignment.builder()
                .gig(GIG).songId(song.getSongId()).set(set).build();
        return SetlistEntry.builder().song(song).assignment(assignment).build();
    }

    private void givenSetlist(SetlistEntry... entries) {
        when(assembler.assemble(any(), eq(false)))
                .thenReturn(Setlist.builder().gig(GIG).entries(List.of(entries)).build());
    }

    @Test
    @SuppressWarnings("unchecked")
    void writesTheSetsAndTheGigsVenueToThePort() {
        givenSetlist(entry("A01", "One"), entry("A02", "Two"), entry("B01", "Three"));
        when(gigVenuePort.readGigVenues(any())).thenReturn(Map.of(GIG, "Moods Wine Bar"));

        Setlist result = service.printFanSetlist(GIG, "./out/fans.html");

        ArgumentCaptor<List<List<SetlistEntry>>> sets = ArgumentCaptor.forClass(List.class);
        verify(sheetPort).writeFanSetlistSheet(eq(Path.of("./out/fans.html")), eq(GIG),
                eq("Moods Wine Bar"), sets.capture());
        assertThat(sets.getValue()).extracting(List::size).containsExactly(2, 1);
        assertThat(result.size()).isEqualTo(3);
    }

    @Test
    void aGigWithNoVenueIsStillPrinted_withANullVenue() {
        givenSetlist(entry("A01", "One"));
        when(gigVenuePort.readGigVenues(any())).thenReturn(Map.of());

        service.printFanSetlist(null, "./out/fans.html");

        verify(sheetPort).writeFanSetlistSheet(any(), eq(GIG), eq(null), any());
    }

    @Test
    void neverAsksForBackupSongs() {
        givenSetlist(entry("A01", "One"));
        when(gigVenuePort.readGigVenues(any())).thenReturn(Map.of());

        service.printFanSetlist(GIG, "./out/fans.html");

        verify(assembler).assemble(GIG, false);
    }
}
