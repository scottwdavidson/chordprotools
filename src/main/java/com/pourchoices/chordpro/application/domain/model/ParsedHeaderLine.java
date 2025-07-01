package com.pourchoices.chordpro.application.domain.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class ParsedHeaderLine implements Comparable<ParsedHeaderLine> {

    @NonNull
    HeaderDirective headerDirective;

    @NonNull
    String value;

    @Override
    public String toString() {

        StringBuilder builder = new StringBuilder();

        // opening {
        builder.append("{");

        // if meta, add meta prefix first
        if (headerDirective.isMeta()) {
            builder.append(HeaderDirective.UNPARSED_META.getPrefixes().get(0));
            builder.append(": ");
        }

        // prefix {<prefix>:
        builder.append(headerDirective.getPrefixes().get(0));
        builder.append(": ");

        // value
        builder.append(value);
        builder.append("}");

        return builder.toString();

    }

    @Override
    public int compareTo(ParsedHeaderLine o) {
        return o.getHeaderDirective().getCardinality() - this.headerDirective.getCardinality();
    }
}
