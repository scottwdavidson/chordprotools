package com.pourchoices.chordpro.domain.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ParsedSong {

    @NonNull
    ParsedHeader parsedHeader;

    @Singular
    List<String> lines;

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        // header
        builder.append(parsedHeader.toString());
        builder.append("\n");

        // the rest
        for (String line : lines ){
            builder.append(line).append("\n");
        }

        return builder.toString();
    }
}
