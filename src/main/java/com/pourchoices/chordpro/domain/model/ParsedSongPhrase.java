package com.pourchoices.chordpro.domain.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ParsedSongPhrase {

    @NonNull
    SongDirective songDirective;

    @Singular
    List<String> lines;

}
