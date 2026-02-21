package com.pourchoices.chordpro.application.domain.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;

@Getter
public enum SongDirective {
  SONG_COMMENT(List.of("c", "comment"), List.of(), true),
  DOCUMENT_COMMENT(List.of("{"), List.of("}"), true),
  VERSE(List.of("start_of_verse", "sov"), List.of("end_of_verse", "eov")),
  CHORUS(List.of("start_of_chorus", "soc"), List.of("end_of_chorus", "eoc")),
  BRIDGE(List.of("start_of_bridge", "sob"), List.of("end_of_bridge", "eob")),
  PART(List.of("start_of_part", "sop"), List.of("end_of_part", "eop"));

  private static final Map<String, SongDirective> lookup = new HashMap<>();

  static {
    for (SongDirective SongDirective : SongDirective.values()) {
      for (String key : SongDirective.prefixes) {
        lookup.put(key, SongDirective);
      }
    }
  }

  private final List<String> prefixes;
  private final List<String> suffixes;
  private final boolean singleLineDirective;

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