package com.pourchoices.chordpro.application.domain.model;

import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the mandatory and optional header directives recognized in a Pour Choices chordpro file.
 *
 * Note: Cardinality is used for comparison and therefore must be unique otherwise the directives will be
 * managed by the SortedSet as the same entry.
 *j
 */
@Getter
public enum HeaderDirective {
    TITLE(List.of("title"),   90),
    ARTIST(List.of("artist"), 89),
    KEY(List.of("key"), 88),
    DURATION(List.of("duration"), 87),
    TEMPO(List.of("tempo"), 86),
    TIME_SIGNATURE(List.of("time"), false,85),
    CAPO(List.of("capo","Capo"),   false, 60),
    NORD(List.of("nord", "Nord", "N"), "null", true, true, 50),
    ROLAND(List.of("roland"), "null", true, true, 49),
    VERSION(List.of("version"), "0.0", true, true, 44),
    COUNTIN(List.of("countin", "Countin", "CountIn"), "24", false, true, 42),
    BACKING(List.of("backing", "Backing Track"), "99", false, true, 30),
    VE(List.of("ve", "VE"), "U99", false, true, 28),
    PERFORMANCE_KEY(List.of("performance", "performanceKey", "PerformanceKey", "Performance Key"), "Hm", false, true, 20),
    EPHEMERAL_COMMENT(List.of("**"), "null", false, false, 10),
    UNPARSED_META(List.of("meta"),  "0", false, false,1 );

    private final List<String> prefixes;
    private final String nullValue;
    private final boolean mandatory;
    private final boolean meta;
    private final Integer cardinality;

    private static final Map<String, HeaderDirective> lookup = new HashMap<>();

    static {
        for (HeaderDirective directive : HeaderDirective.values()) {
            for (String key : directive.prefixes) {
                lookup.put(key, directive);
            }
        }
    }

    HeaderDirective(List<String> prefixes, Integer cardinality) {

        this(prefixes, "null", true, false, cardinality);
    }

    HeaderDirective(List<String> prefixes, boolean meta, Integer cardinality) {
        this(prefixes, "null", false, meta, cardinality);
    }

    HeaderDirective(List<String> prefixes, String nullValue, boolean mandatory, boolean meta, Integer cardinality) {
        this.prefixes = prefixes;
        this.nullValue = nullValue;
        this.mandatory = mandatory;
        this.meta = meta;
        this.cardinality = cardinality;
    }

    public static HeaderDirective getByPrefix(String prefix) {
        return lookup.get(prefix);
    }

}