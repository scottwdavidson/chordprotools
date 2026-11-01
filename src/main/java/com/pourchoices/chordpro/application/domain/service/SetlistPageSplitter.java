package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.SetlistEntry;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Paginates a setlist for the floor sheet: every page holds songs from exactly one set, with
 * at most {@code maxRowsPerPage} songs, so the type can be huge and "which page am I on" is
 * always "which set am I in".
 *
 * <p>A set that needs more than one page is split into evenly sized pages: 17 songs at max 6
 * becomes 6/6/5, never 6/6/4/1 with a lonely last page. Pure logic, no I/O.
 */
@Component
public class SetlistPageSplitter {

    public List<List<SetlistEntry>> paginate(List<SetlistEntry> orderedEntries, int maxRowsPerPage) {
        if (maxRowsPerPage < 1) {
            throw new IllegalArgumentException("maxRowsPerPage must be at least 1, was " + maxRowsPerPage);
        }
        List<List<SetlistEntry>> pages = new ArrayList<>();
        for (List<SetlistEntry> set : groupBySet(orderedEntries)) {
            pages.addAll(splitEvenly(set, maxRowsPerPage));
        }
        return pages;
    }

    /** Consecutive runs of entries sharing the same set letter, in order. */
    private List<List<SetlistEntry>> groupBySet(List<SetlistEntry> entries) {
        List<List<SetlistEntry>> sets = new ArrayList<>();
        char previous = 0;
        for (SetlistEntry entry : entries) {
            char letter = entry.getSet().charAt(0);
            if (letter != previous) sets.add(new ArrayList<>());
            sets.get(sets.size() - 1).add(entry);
            previous = letter;
        }
        return sets;
    }

    /** Fewest pages that respect the cap, with sizes differing by at most one (bigger pages first). */
    private List<List<SetlistEntry>> splitEvenly(List<SetlistEntry> set, int maxRows) {
        int pageCount = (set.size() + maxRows - 1) / maxRows;
        int base = set.size() / pageCount;
        int extra = set.size() % pageCount;

        List<List<SetlistEntry>> pages = new ArrayList<>();
        int from = 0;
        for (int i = 0; i < pageCount; i++) {
            int to = from + base + (i < extra ? 1 : 0);
            pages.add(set.subList(from, to));
            from = to;
        }
        return pages;
    }
}
