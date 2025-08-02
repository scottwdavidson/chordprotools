package com.pourchoices.chordpro.application.domain.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

import java.util.List;
import java.util.SortedSet;

@Value
@Builder
public class ParsedHeader implements Comparable<ParsedHeader> {

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

                builder.append("{c:**  ").append(parsedHeaderLineNoCurlies).append("   }\n");
            }
        }
        builder.append("{c:***********************************************}\n");

        return builder.toString();
    }

    @Override
    public int compareTo(@NonNull ParsedHeader o) {

        int filenameComparison = this.chordProFilename.compareTo(o.chordProFilename);
        if (filenameComparison != 0) {
            return filenameComparison;
        }

        List<ParsedHeaderLine> thisHeaderList = this.headerLines.stream().toList();
        List<ParsedHeaderLine> otherHeaderList = o.headerLines.stream().toList();

        // Compare sizes first
        int sizeComparison = Integer.compare(thisHeaderList.size(), otherHeaderList.size());
        if (sizeComparison != 0) {
            return sizeComparison;
        }

        // If sizes are equal, compare elements one by one
        for (int i = 0; i < thisHeaderList.size(); i++) {
            int elementComparison = thisHeaderList.get(i).compareTo(otherHeaderList.get(i));
            if (elementComparison != 0) {
                return elementComparison;
            }
        }

        // If all fields are equal, return 0 (consistent with Lombok's generated equals)
        return 0;

    }

    @NonNull
    String chordProFilename;

    @Singular
    @NonNull
    SortedSet<ParsedHeaderLine> headerLines;


}
