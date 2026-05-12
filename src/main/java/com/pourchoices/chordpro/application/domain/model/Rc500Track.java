package com.pourchoices.chordpro.application.domain.model;

import lombok.Builder;
import lombok.Value;

/**
 * Domain model for one track of an RC-500 memory slot.
 *
 * <p>The RC-500 provides two independent tracks per memory slot.  In this band's
 * setup:
 * <ul>
 *   <li><b>Track 1</b> — the backing track (drums, bass, keys, etc.).</li>
 *   <li><b>Track 2</b> — the click track.</li>
 * </ul>
 *
 * <p>Only the fields relevant to the backing-track automation workflow are surfaced
 * here.  The full XML representation (including pan, loop-FX enable, start/stop mode,
 * etc.) lives in {@link com.pourchoices.chordpro.adapter.out.file.Rc500TrackDto}.
 */
@Value
@Builder
public class Rc500Track {

    /**
     * Playback level, 0–200; 100 = unity gain.
     * Sourced from the RC0 XML element {@code <PlyLvl>}.
     */
    int playLevel;

    /**
     * Wave status as stored by the device.
     * {@code 0} = no audio present; any other value = audio is loaded.
     * Sourced from the RC0 XML element {@code <WavStat>}.
     */
    int wavStat;

    /**
     * Wave length as stored by the device (units are device-internal).
     * Sourced from the RC0 XML element {@code <WavLen>}.
     */
    int wavLen;

    /**
     * Convenience predicate — {@code true} when the track has audio loaded.
     */
    public boolean hasAudio() {
        return wavStat != 0;
    }
}
