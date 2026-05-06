package com.pourchoices.chordpro.application.domain.model;

import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * A formalized domain object representing the performance setlist — that is, the
 * subset of catalog entries which have a non-blank "set" value, ordered by that value.
 *
 * <p>The {@code set} field on each entry uses a sortable code such as "A01", "A02",
 * "B01", etc., so a natural lexicographic sort produces the correct song order
 * across sets and within each set.
 *
 * <p>Additional setlist-aware capabilities (e.g. total runtime, per-set breakdown,
 * PDF printing helpers) should be built by consuming this object rather than
 * re-filtering the raw catalog map.
 */
@Value
@Builder
public class Setlist {

    /** Songs in set-order.  Never null; may be empty if nothing is assigned to a set. */
    List<CatalogEntry> entries;

    /** Convenience accessor — total number of songs in the setlist. */
    public int size() {
        return entries.size();
    }
}
