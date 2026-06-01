package com.pourchoices.chordpro.adapter.in.file;

import com.pourchoices.chordpro.application.domain.model.SongMatch;
import com.pourchoices.chordpro.application.port.in.FindSongIdUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.List;

/**
 * CLI command that searches {@code song-catalog.csv} by a title-or-artist
 * fragment and prints one row per song, with the SONG ID to paste into
 * {@code gigs.csv}.
 *
 * <pre>
 *   ./find-song-id "joel"
 *   ./find-song-id "piano"
 * </pre>
 *
 * <p>Exits with code 1 if nothing matches, so the result is scriptable.
 */
@Component
@Command(
        name = "find-song-id",
        description = "Searches song-catalog.csv by title or artist fragment. "
                + "Prints one row per song (base version) with the SONG ID to paste "
                + "into gigs.csv, annotated with key-variant counts."
)
@Slf4j
public class FindSongIdCommand implements Runnable {

    private static final String ROW_FMT = "%-40s  %-25s  %-5s  %-40s  %s%n";

    private final FindSongIdUseCase useCase;

    public FindSongIdCommand(FindSongIdUseCase useCase) {
        this.useCase = useCase;
    }

    @Parameters(index = "0", description = "Title or artist fragment to search for (case-insensitive).")
    private String fragment;

    @Override
    public void run() {
        List<SongMatch> matches = useCase.findByFragment(fragment);

        if (matches.isEmpty()) {
            System.out.printf("%nNo songs found matching '%s'%n%n", fragment);
            System.exit(1);
        }

        System.out.println();
        System.out.printf(ROW_FMT, "TITLE", "ARTIST", "KEY", "SONG ID (paste into setlist)", "VARIANTS");
        System.out.println("-".repeat(120));
        for (SongMatch m : matches) {
            System.out.printf(ROW_FMT,
                    truncate(m.getTitle(), 40),
                    truncate(m.getArtist(), 25),
                    truncate(nullToEmpty(m.getKey()), 5),
                    m.getDisplaySongId(),
                    variantNote(m));
        }
        System.out.println();
    }

    private static String variantNote(SongMatch m) {
        if (m.isOrphan()) {
            return "[!] ID has key suffix — data needs fix before use in setlist";
        }
        int n = m.getVariantCount();
        if (n == 0) {
            return "";
        }
        return "+" + n + " key variant" + (n != 1 ? "s" : "");
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
