package com.pourchoices.chordpro.adapter.out.file;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * XML DTO for one {@code <mem id="N">} element inside an RC-500 {@code .RC0} file.
 *
 * <p>Captures the complete per-slot XML structure so that round-trip read/write
 * preserves every RC-500 setting, including sections the domain model does not
 * explicitly model (rhythm, loop-FX, assignable controllers, etc.).
 *
 * <p>The {@code name} field stores the raw 12 ASCII integer codes from the
 * {@code <NAME><C01>…<C12>} XML elements.  The mapper converts these to/from
 * a human-readable {@link String} when mapping to/from the domain model.
 *
 * <p>Assign entries are stored in index order (index 0 = ASSIGN1, …, index 7 = ASSIGN8).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Rc500SlotDto {

    // -----------------------------------------------------------------------
    // Slot identity
    // -----------------------------------------------------------------------

    /** Zero-based slot index; matches the {@code id} attribute of {@code <mem>}. */
    int slotIndex;

    /**
     * Raw 12-element array of ASCII integer character codes for the slot name.
     * Positions beyond the actual name text are padded with 32 (space).
     * Example: "HeyJude     " → {72,101,121,74,117,100,101,32,32,32,32,32}.
     */
    @Builder.Default
    int[] nameCodes = new int[12];

    // -----------------------------------------------------------------------
    // Track sections
    // -----------------------------------------------------------------------

    @Builder.Default Rc500TrackDto track1 = Rc500TrackDto.builder().build();
    @Builder.Default Rc500TrackDto track2 = Rc500TrackDto.builder().build();

    // -----------------------------------------------------------------------
    // MASTER section
    // -----------------------------------------------------------------------

    /** Tempo × 10 (e.g. 1200 = 120.0 BPM). */
    @Builder.Default int masterTempo            = 1200;
    @Builder.Default int masterDubMode          = 0;
    @Builder.Default int masterRecAction        = 1;
    @Builder.Default int masterRecQuantize      = 0;
    @Builder.Default int masterAutoRec          = 0;
    @Builder.Default int masterAutoRecSens      = 50;
    @Builder.Default int masterAutoRecSrc       = 0;
    @Builder.Default int masterPlayMode         = 0;
    /** Note: RC0 XML spells this "SinglPlayeChange" (firmware typo preserved). */
    @Builder.Default int masterSinglPlayeChange = 0;
    @Builder.Default int masterFadeTime         = 5;
    @Builder.Default int masterAllStart         = 0;
    @Builder.Default int masterTrackChain       = 0;
    @Builder.Default int masterCurrentTrack     = 0;
    @Builder.Default int masterAllTrackSel      = 0;
    @Builder.Default int masterLevel            = 100;
    @Builder.Default int masterLpMod            = 0;
    @Builder.Default int masterLpLen            = 0;
    @Builder.Default int masterTrkMod           = 1;
    @Builder.Default int masterSync             = 0;

    // -----------------------------------------------------------------------
    // LOOPFX section
    // -----------------------------------------------------------------------

    @Builder.Default int loopFxSw              = 0;
    @Builder.Default int loopFxType            = 4;
    @Builder.Default int loopFxRepeatLength    = 3;
    /** Note: RC0 XML spells this "ShiftShift" (firmware quirk preserved). */
    @Builder.Default int loopFxShiftShift      = 3;
    @Builder.Default int loopFxScatterLength   = 2;
    /** Note: RC0 XML spells this "VinylFlickFlick" (firmware quirk preserved). */
    @Builder.Default int loopFxVinylFlickFlick = 50;

    // -----------------------------------------------------------------------
    // RHYTHM section
    // -----------------------------------------------------------------------

    @Builder.Default int rhythmLevel           = 100;
    @Builder.Default int rhythmReverb          = 30;
    @Builder.Default int rhythmPattern         = 0;
    @Builder.Default int rhythmVariation       = 0;
    @Builder.Default int rhythmVariationChange = 0;
    @Builder.Default int rhythmKit             = 0;
    @Builder.Default int rhythmBeat            = 2;
    @Builder.Default int rhythmFill            = 1;
    @Builder.Default int rhythmPart1           = 1;
    @Builder.Default int rhythmPart2           = 1;
    @Builder.Default int rhythmPart3           = 1;
    @Builder.Default int rhythmPart4           = 0;
    @Builder.Default int rhythmRecCount        = 0;
    @Builder.Default int rhythmPlayCount       = 0;
    @Builder.Default int rhythmStart           = 0;
    @Builder.Default int rhythmStop            = 1;
    @Builder.Default int rhythmToneLow         = 10;
    @Builder.Default int rhythmToneHigh        = 10;
    @Builder.Default int rhythmState           = 0;

    // -----------------------------------------------------------------------
    // CTL section (physical pedal/control assignments)
    // -----------------------------------------------------------------------

    @Builder.Default int ctlPedal1 = 28;
    @Builder.Default int ctlPedal2 = 36;
    @Builder.Default int ctlPedal3 = 27;
    @Builder.Default int ctlCtl1   = 44;
    @Builder.Default int ctlCtl2   = 53;
    @Builder.Default int ctlExp    = 13;

    // -----------------------------------------------------------------------
    // ASSIGN1–8 sections (6 fields each)
    // -----------------------------------------------------------------------

    /**
     * Eight assignable-controller entries (ASSIGN1 = index 0, …, ASSIGN8 = index 7).
     * Each entry contains: sw, source, sourceMode, target, targetMin, targetMax.
     */
    @Builder.Default
    List<Rc500AssignDto> assigns = defaultAssigns();

    // -----------------------------------------------------------------------
    // Factory helpers
    // -----------------------------------------------------------------------

    private static List<Rc500AssignDto> defaultAssigns() {
        List<Rc500AssignDto> list = new ArrayList<>(8);
        for (int i = 0; i < 8; i++) {
            list.add(Rc500AssignDto.builder().build());
        }
        return list;
    }
}
