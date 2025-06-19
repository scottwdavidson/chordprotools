package com.pourchoices.chordpro.domain.model;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.SortedSet;

@Value
@Builder
public class ParsedHeader {

    @Singular
    SortedSet<ParsedHeaderLine> headerLines;

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        // core header and metas
        for (ParsedHeaderLine parsedHeaderLine : headerLines) {
            builder.append(parsedHeaderLine.toString()).append("\n");
        }

        // metas as presentation comments
        builder.append("\n");
        builder.append("{c:***********************************************}\n");
        for (ParsedHeaderLine parsedHeaderLine : headerLines) {
            if (parsedHeaderLine.getHeaderDirective().isMeta()) {
                String parsedHeaderLineNoCurlies = parsedHeaderLine.toString();
                parsedHeaderLineNoCurlies = parsedHeaderLineNoCurlies.replaceAll("^\\{|\\}$", "");

                builder.append("{c:** --> ").append(parsedHeaderLineNoCurlies).append("}\n");
            }
        }
        builder.append("{c:***********************************************}\n");

        return builder.toString();
    }
}
