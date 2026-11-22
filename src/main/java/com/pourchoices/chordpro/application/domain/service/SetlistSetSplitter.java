package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.SetlistEntry;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Groups a setlist by set, in order: every song whose set code starts with {@code A} is one
 * group, every {@code B} song the next, and so on. So three sets are three groups, two sets
 * are two. Sets are never merged or split — a set is a set, however long it runs.
 *
 * <p>Pure logic, no I/O.
 */
@Component
public class SetlistSetSplitter {

    /** @param orderedEntries entries already sorted by set code; the order is preserved */
    public List<List<SetlistEntry>> splitBySet(List<SetlistEntry> orderedEntries) {
        Map<Character, List<SetlistEntry>> bySet = new LinkedHashMap<>();
        for (SetlistEntry entry : orderedEntries) {
            bySet.computeIfAbsent(entry.getSet().charAt(0), set -> new ArrayList<>()).add(entry);
        }
        return new ArrayList<>(bySet.values());
    }
}
