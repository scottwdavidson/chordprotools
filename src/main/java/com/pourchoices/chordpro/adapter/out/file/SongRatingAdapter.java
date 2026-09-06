package com.pourchoices.chordpro.adapter.out.file;

import com.pourchoices.chordpro.application.domain.model.SongRating;
import com.pourchoices.chordpro.application.port.out.SongRatingPort;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;

@Service
public class SongRatingAdapter implements SongRatingPort {

    private final SongRatingFileReader reader;

    public SongRatingAdapter(SongRatingFileReader reader) {
        this.reader = reader;
    }

    @Override
    public List<SongRating> readRatings(Path path) {
        return reader.readRatings(path);
    }
}
