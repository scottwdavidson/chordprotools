package com.pourchoices.chordpro.application.domain.model;

import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * A formalized domain object representing the performance setlist for a specific gig.
 *
 * <p>Entries are {@link SetlistEntry} objects — a joined view of catalog metadata
 * and gig assignment — sorted by set code (e.g. A01, A02, B01).
 *
 * <p>Additional setlist-aware capabilities (e.g. total runtime, per-set breakdown,
 * PDF printing helpers) should be built by consuming this object rather than
 * re-filtering the raw catalog map.
 */
@Value
@Builder
public class Setlist {

    /** Gig identifier slug for which this setlist was generated. */
    String gig;

    /** Songs in set-order.  Never null; may be empty if nothing is assigned to a set. */
    List<SetlistEntry> entries;

    /** Convenience accessor — total number of songs in the setlist. */
    public int size() {
        return entries.size();
    }
}
