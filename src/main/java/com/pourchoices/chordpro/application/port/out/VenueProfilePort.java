package com.pourchoices.chordpro.application.port.out;

import com.pourchoices.chordpro.application.domain.model.VenueProfile;

import java.nio.file.Path;
import java.util.Map;

/**
 * Output port for reading {@code venue-profiles.csv}.
 *
 * <p>Read-only by design: unlike {@code song-catalog.csv} or
 * {@code gigs.csv}, nothing in the pipeline programmatically writes this
 * file (yet) — it's hand-curated the same way, but no command needs a
 * write path until some future venue-policy-editing tool earns its keep.
 * Add {@code writeVenueProfiles} then, not speculatively now.
 */
public interface VenueProfilePort {

    /**
     * @return all configured venue profiles, keyed by gig slug. A gig with
     *     no row in the file simply has no entry — this is not an error.
     *     Returns an empty map if the file doesn't exist yet.
     */
    Map<String, VenueProfile> readVenueProfiles(Path path);
}
