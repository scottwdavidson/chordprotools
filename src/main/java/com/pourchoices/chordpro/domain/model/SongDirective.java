package com.pourchoices.chordpro.domain.model;

import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public enum SongDirective {
    SONG_COMMENT(List.of("c","comment"),  List.of(), true),
    DOCUMENT_COMMENT(List.of("{"), List.of("}"), true),
    VERSE(List.of("start_of_verse","sov"), List.of("end_of_verse","eov")),
    CHORUS(List.of("start_of_chorus","soc"), List.of("end_of_chorus","eoc")),
    BRIDGE(List.of("start_of_bridge","sob"), List.of("end_of_bridge","eob"));

    private final List<String> prefixes;
    private final List<String> suffixes;
    private final boolean singleLineDirective;

    private static final Map<String, SongDirective> lookup = new HashMap<>();

    static {
        for (SongDirective SongDirective : SongDirective.values()) {
            for (String key : SongDirective.prefixes) {
                lookup.put(key, SongDirective);
            }
        }
    }

    SongDirective(List<String> prefixes, List<String> suffixes) {
        this(prefixes, suffixes, false);
    }

        SongDirective(List<String> prefixes, List<String> suffixes, boolean singleLineDirective) {
        this.prefixes = prefixes;
        this.suffixes = suffixes;
        this.singleLineDirective = singleLineDirective;
    }

    public static SongDirective getByPrefix(String prefix) {
        return lookup.get(prefix);
    }

}