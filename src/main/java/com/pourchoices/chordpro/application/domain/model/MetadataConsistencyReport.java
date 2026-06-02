package com.pourchoices.chordpro.application.domain.model;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

/**
 * Immutable result of a {@code consistent-metadata} run.
 *
 * <p>Holds the findings for every song group that had a problem, plus summary
 * counters for the report footer and exit code.
 */
@Value
@Builder
public class MetadataConsistencyReport {

    /** The kind of inconsistency found within a song group. */
    public enum FindingType {
        /** A non-key/-capo field differs between variants of the same song. */
        DRIFT,
        /** A variant's filename key suffix disagrees with its {@code {key:}}. */
        FILENAME_KEY
    }

    /** One inconsistency for a single song group. */
    @Value
    @Builder
    public static class Finding {
        FindingType type;
        /** Group key (base song ID) the finding belongs to. */
        String groupKey;
        /** Human-readable detail lines (already formatted). */
        @Singular("detail")
        List<String> details;
    }

    @Singular
    List<Finding> findings;

    /** Total song groups examined (groups with 2+ variants for DRIFT). */
    int groupsChecked;

    /** Groups with no findings. */
    int consistentGroups;

    /** Number of groups with at least one finding (drives the exit code). */
    public int issueCount() {
        return findings.size();
    }
}
