package com.pourchoices.chordpro.application.port.out;

import java.nio.file.Path;
import java.util.Map;

/**
 * Output port for reading {@code gig-venues.csv} — the thin association
 * between a gig slug and the venue name it's played (or will be played) at.
 *
 * <p>Deliberately just {@code Map<String, String>} (gig -> venue name), not
 * a richer domain type: this file has exactly one piece of information (an
 * association) with no parsing, validation, or business behavior beyond
 * "trim and require non-blank" — a dedicated domain class here would just
 * duplicate the DTO for no benefit. Contrast with {@link VenueProfilePort},
 * whose value type has real fields (vocal intensity, energy range) worth
 * modeling.
 *
 * <p>Not every gig needs an entry — a gig with no row here just doesn't
 * have a venue attached yet (e.g. a candidate setlist being pitched to a
 * venue before a gig date is confirmed). Not every venue needs a gig either
 * — see {@link VenueProfilePort} for pre-registering a venue's policy ahead
 * of any booking.
 */
public interface GigVenuePort {

    /** @return gig slug -> venue name. Returns an empty map if the file doesn't exist yet. */
    Map<String, String> readGigVenues(Path path);
}
