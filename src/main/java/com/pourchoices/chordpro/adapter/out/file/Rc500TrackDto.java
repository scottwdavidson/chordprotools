package com.pourchoices.chordpro.adapter.out.file;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * XML DTO for the {@code <TRACK1>} and {@code <TRACK2>} elements inside an
 * RC-500 {@code <mem>} node.
 *
 * <p>Field names match the RC0 XML tag names (camelCase-ified).  All values
 * default to the factory-reset defaults so that a newly constructed DTO is
 * immediately valid for serialization.
 *
 * <p>Semantics of notable fields:
 * <ul>
 *   <li>{@code plyLvl} — playback level 0–200, 100 = unity.</li>
 *   <li>{@code pan} — stereo pan 0–100, 50 = centre.</li>
 *   <li>{@code loopFx} — 1 = loop-FX section is active for this track.</li>
 *   <li>{@code wavStat} — 0 = no audio; non-zero = audio loaded / recorded.</li>
 *   <li>{@code wavLen} — device-internal audio length value.</li>
 *   <li>{@code recTmp} — record tempo × 10 (e.g. 1200 = 120.0 BPM).</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Rc500TrackDto {

    // -----------------------------------------------------------------------
    // RC0 field defaults (factory-reset state)
    // -----------------------------------------------------------------------

    @Builder.Default int rev        = 0;
    @Builder.Default int plyLvl    = 100;
    @Builder.Default int pan        = 50;
    @Builder.Default int one        = 0;
    @Builder.Default int loopFx     = 1;
    @Builder.Default int strtMod    = 0;
    @Builder.Default int stpMod     = 0;
    @Builder.Default int measure    = 1;
    @Builder.Default int loopSync   = 1;
    @Builder.Default int tempoSync  = 1;
    @Builder.Default int input      = 0;
    @Builder.Default int output     = 0;
    @Builder.Default int measMod    = 1;
    @Builder.Default int measLen    = 0;
    @Builder.Default int measBtLp   = 0;
    @Builder.Default int recTmp     = 1200;
    @Builder.Default int wavStat    = 0;
    @Builder.Default int wavLen     = 0;
}
