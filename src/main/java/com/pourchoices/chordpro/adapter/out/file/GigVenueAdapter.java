package com.pourchoices.chordpro.adapter.out.file;

import com.pourchoices.chordpro.application.port.out.GigVenuePort;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.Map;

/**
 * Adapter implementing {@link GigVenuePort} via CSV file I/O.
 */
@Service
public class GigVenueAdapter implements GigVenuePort {

    private final GigVenueFileReader reader;

    public GigVenueAdapter(GigVenueFileReader reader) {
        this.reader = reader;
    }

    @Override
    public Map<String, String> readGigVenues(Path path) {
        return reader.readGigVenues(path);
    }
}
