package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.SetlistEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * De-duplicates a list of {@link SetlistEntry} objects that may contain the
 * same song assigned to a gig more than once (human-entry error).
 *
 * <p>Since {@code gigs.csv} enforces base-version-only SONG IDs,
 * key-variant collisions cannot occur here.  The only realistic duplicate is the
 * same base SONG ID appearing twice in one gig's assignments (e.g., after a
 * copy-and-edit mistake).  In that case the first occurrence is kept and a
 * warning is logged.
 *
 * <p>Grouping is done on {@link com.pourchoices.chordpro.application.domain.model.SongId#toGroupKey()}
 * which is identical to {@code toString()} for base versions, keeping the
 * implementation consistent with the broader codebase convention.
 */
@Component
@Slf4j
public class SetlistDeduplicator {

    /**
     * Returns a de-duplicated list, preserving insertion order.
     * If the same song appears more than once, the first entry wins and the
     * rest are logged as warnings.
     */
    public List<SetlistEntry> deduplicate(List<SetlistEntry> entries) {
        Map<String, SetlistEntry> seen = new LinkedHashMap<>();

        for (SetlistEntry entry : entries) {
            String key = entry.getSongId().toGroupKey();
            if (seen.containsKey(key)) {
                log.warn("De-dup: '{}' appears more than once in this gig's assignments "
                        + "(set '{}' vs set '{}'). Keeping first occurrence.",
                        key,
                        seen.get(key).getSet(),
                        entry.getSet());
            } else {
                seen.put(key, entry);
            }
        }

        return new ArrayList<>(seen.values());
    }
}
