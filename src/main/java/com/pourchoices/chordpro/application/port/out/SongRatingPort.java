package com.pourchoices.chordpro.application.port.out;

import com.pourchoices.chordpro.application.domain.model.SongRating;

import java.nio.file.Path;
import java.util.List;

/**
 * Output port for reading a {@code characterize-songs} ratings input file.
 *
 * <p>Unlike the other file ports, this one's path is always caller-supplied
 * (a CLI argument), not a fixed config path — the ratings file is a one-off
 * curated artifact, not a permanent pipeline file like {@code gigs.csv}.
 */
public interface SongRatingPort {

    /** @return all rating rows in file order. Returns an empty list if the file doesn't exist. */
    List<SongRating> readRatings(Path path);
}
