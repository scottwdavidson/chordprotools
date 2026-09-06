package com.pourchoices.chordpro.adapter.out.file;

import com.pourchoices.chordpro.application.domain.model.VenueProfile;
import com.pourchoices.chordpro.application.port.out.VenueProfilePort;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.Map;

/**
 * Adapter implementing {@link VenueProfilePort} via CSV file I/O.
 * Matches the pattern used by {@link CatalogAdapter} / {@link SetlistAssignmentsAdapter}.
 */
@Service
public class VenueProfileAdapter implements VenueProfilePort {

    private final VenueProfileFileReader reader;

    public VenueProfileAdapter(VenueProfileFileReader reader) {
        this.reader = reader;
    }

    @Override
    public Map<String, VenueProfile> readVenueProfiles(Path path) {
        return reader.readVenueProfiles(path);
    }
}
