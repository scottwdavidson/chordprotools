package com.pourchoices.chordpro.application.domain.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ParsedSong {

    public ParsedSong withHeader(ParsedHeader newHeader) {
        return ParsedSong.builder()
                .parsedHeader(newHeader)
                .lines(getLines())
                .build();
    }

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

    @NonNull
    ParsedHeader parsedHeader;

    @Singular
    List<String> lines;

}
