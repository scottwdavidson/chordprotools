package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.GuitarSwitch;
import com.pourchoices.chordpro.application.domain.model.GuitarType;
import com.pourchoices.chordpro.application.domain.model.SetlistEntry;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure instrument-switch detection for {@code evaluate-setlist}. No I/O —
 * takes an already-ordered, already-deduplicated {@link SetlistEntry} list
 * (fan-facing sets only; Z-set backups are the caller's responsibility to
 * filter out first, same convention as {@link SetlistArcScorer}).
 *
 * <p>A switch is only flagged when BOTH neighbouring songs have a known,
 * specific {@link GuitarType} and they differ. A song with no guitar type
 * characterized yet (null) or deliberately marked {@link GuitarType#UNKNOWN}
 * tells us nothing actionable, so it's silently skipped rather than treated
 * as a false-positive switch — same "degrade gracefully on missing data"
 * philosophy as the rest of the characterization checks.
 */
@Component
public class GuitarSwitchScorer {

    /** @return one {@link GuitarSwitch} per adjacent pair of songs that requires a different guitar. */
    public List<GuitarSwitch> detectSwitches(List<SetlistEntry> orderedEntries) {
        List<GuitarSwitch> switches = new ArrayList<>();
        for (int i = 1; i < orderedEntries.size(); i++) {
            SetlistEntry previous = orderedEntries.get(i - 1);
            SetlistEntry current = orderedEntries.get(i);

            GuitarType fromGuitar = previous.getSong().getGuitarType();
            GuitarType toGuitar = current.getSong().getGuitarType();

            if (isActionableSwitch(fromGuitar, toGuitar)) {
                switches.add(GuitarSwitch.builder()
                        .set(current.getSet())
                        .fromTitle(previous.getTitle())
                        .fromArtist(previous.getArtist())
                        .fromGuitar(fromGuitar)
                        .toTitle(current.getTitle())
                        .toArtist(current.getArtist())
                        .toGuitar(toGuitar)
                        .build());
            }
        }
        return switches;
    }

    private boolean isActionableSwitch(GuitarType fromGuitar, GuitarType toGuitar) {
        return fromGuitar != null && toGuitar != null
                && fromGuitar != GuitarType.UNKNOWN && toGuitar != GuitarType.UNKNOWN
                && fromGuitar != toGuitar;
    }
}
