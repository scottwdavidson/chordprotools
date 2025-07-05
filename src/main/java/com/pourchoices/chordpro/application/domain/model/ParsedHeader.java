package com.pourchoices.chordpro.application.domain.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

import java.util.SortedSet;

@Value
@Builder
public class ParsedHeader {

    String chordProFilename;

    @Singular
    SortedSet<ParsedHeaderLine> headerLines;

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        // core header and metas
        for (ParsedHeaderLine parsedHeaderLine : headerLines) {

            // skip ephemeral comments, but add everything else
            if (parsedHeaderLine.getHeaderDirective() != HeaderDirective.EPHEMERAL_COMMENT) {
                builder.append(parsedHeaderLine.toString()).append("\n");
            }
        }

        // metas as ephemeral comments
        builder.append("\n");
        builder.append("{c:***********************************************}\n");
        for (ParsedHeaderLine parsedHeaderLine : headerLines) {
            if (parsedHeaderLine.getHeaderDirective().isMeta()) {
                String parsedHeaderLineNoCurlies = parsedHeaderLine.toString();
                parsedHeaderLineNoCurlies = parsedHeaderLineNoCurlies.replaceAll("^\\{|\\}$", "");

                builder.append("{c:**  ").append(parsedHeaderLineNoCurlies).append("}\n");
            }
        }
        builder.append("{c:***********************************************}\n");

        return builder.toString();
    }
}
