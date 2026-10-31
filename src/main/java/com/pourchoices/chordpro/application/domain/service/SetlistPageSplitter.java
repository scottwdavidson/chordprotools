package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.SetlistEntry;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Splits an ordered setlist across two printed pages with (as near as possible) the same
 * number of songs on each, so the type can be as big as possible and both pages look alike.
 *
 * <p>The split deliberately ignores set boundaries: readability on stage beats a tidy page
 * turn. With an odd count the first page gets the extra song. Pure logic, no I/O.
 */
@Component
public class SetlistPageSplitter {

    /** @return no pages for an empty list, one page for a single song, otherwise two. */
    public List<List<SetlistEntry>> splitInTwo(List<SetlistEntry> orderedEntries) {
        int size = orderedEntries.size();
        if (size == 0) return List.of();
        if (size == 1) return List.of(orderedEntries);

        int splitAt = (size + 1) / 2;
        return List.of(orderedEntries.subList(0, splitAt), orderedEntries.subList(splitAt, size));
    }
}
