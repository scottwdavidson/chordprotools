package com.pourchoices.chordpro.application.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.With;

/**
 * Domain model for a single RC-500 memory slot.
 *
 * <p>The RC-500 looper pedal stores up to 99 memory slots in each of its
 * {@code MEMORY1.RC0} / {@code MEMORY2.RC0} backup files.  Each slot has a
 * 12-character name (displayed on the device's screen) and two independent
 * audio tracks.  In this band's setup Track 1 carries the <em>backing track</em>
 * and Track 2 carries the <em>click track</em>.
 *
 * <p>Slot indices follow the RC0 XML convention (0-based).  The device displays
 * these as 1-based slot numbers (slot 0 = "Memory 01" on screen).
 *
 * <p>Only the fields relevant to the backing-track automation workflow are
 * captured here.  All other RC-500 per-slot settings (loop FX, rhythm pattern,
 * assignable controllers, etc.) are preserved opaquely inside
 * {@link com.pourchoices.chordpro.adapter.out.file.Rc500SlotDto} during
 * round-trip read/write operations.
 */
@Value
@Builder
public class Rc500Slot {

    /**
     * Zero-based slot index matching the {@code id} attribute of the
     * {@code <mem>} XML element (0–98).
     */
    int slotIndex;

    /**
     * The human-readable name shown on the device — trimmed of trailing spaces.
     * Maximum 12 characters; the RC-500 pads shorter names with spaces.
     * Non-ASCII characters are not supported by the device.
     */
    @With
    String name;

    /**
     * Track 1 — in this band's convention, the <em>backing track</em>
     * (drums, bass, keys, etc.).
     */
    Rc500Track backingTrack;

    /**
     * Track 2 — in this band's convention, the <em>click track</em>.
     */
    Rc500Track clickTrack;

    /**
     * Convenience accessor for the 1-based slot number as displayed on the
     * RC-500 screen (e.g. slot index 0 → "01").
     */
    public int displayNumber() {
        return slotIndex + 1;
    }

    /**
     * Returns {@code true} when at least one track has audio loaded.
     */
    public boolean hasAnyAudio() {
        return backingTrack.hasAudio() || clickTrack.hasAudio();
    }
}
