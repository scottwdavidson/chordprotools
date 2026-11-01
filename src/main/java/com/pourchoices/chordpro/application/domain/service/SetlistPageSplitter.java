package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.SetlistEntry;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits a setlist across the printed floor sheets with one simple formula:
 *
 * <ul>
 *   <li>up to {@code 2 × 16 = 32} songs: {@value #SONGS_PER_SHEET} songs per sheet, so one or two sheets;</li>
 *   <li>more than 32 songs: three sheets, as even as possible (51 songs → 17 / 17 / 17).</li>
 * </ul>
 *
 * <p>Set boundaries are deliberately ignored — a set may continue onto the next sheet; the
 * renderer marks where each new set starts. Pure logic, no I/O.
 */
@Component
public class SetlistPageSplitter {

    /** Standard sheet: most gigs are 3 sets of about 10 songs and fit two sheets of this size. */
    public static final int SONGS_PER_SHEET = 16;

    private static final int STANDARD_SHEET_COUNT = 2;
    private static final int MAX_SHEET_COUNT = 3;

    public List<List<SetlistEntry>> paginate(List<SetlistEntry> orderedEntries) {
        int perSheet = songsPerSheet(orderedEntries.size());

        List<List<SetlistEntry>> sheets = new ArrayList<>();
        for (int from = 0; from < orderedEntries.size(); from += perSheet) {
            int to = Math.min(from + perSheet, orderedEntries.size());
            sheets.add(orderedEntries.subList(from, to));
        }
        return sheets;
    }

    private int songsPerSheet(int totalSongs) {
        if (totalSongs <= SONGS_PER_SHEET * STANDARD_SHEET_COUNT) return SONGS_PER_SHEET;
        return (totalSongs + MAX_SHEET_COUNT - 1) / MAX_SHEET_COUNT;
    }
}
