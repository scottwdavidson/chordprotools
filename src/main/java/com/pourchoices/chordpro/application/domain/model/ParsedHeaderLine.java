package com.pourchoices.chordpro.application.domain.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.Objects;

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
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParsedHeaderLine that = (ParsedHeaderLine) o;

        // Since HeaderDirective is an ENUM, Objects.equals is safer/more explicit than ==
        boolean directiveEquals = Objects.equals(this.headerDirective, that.headerDirective);

        // Your custom logic: Trim and compare the value strings
        boolean valueEquals = this.value.trim().equals(that.value.trim());

        return directiveEquals && valueEquals;
    }

    @Override
    public int hashCode() {

        // Since overriding equals, MUST override hashCode ( and use trimmed value for consistency )
        return Objects.hash(headerDirective, value.trim());
    }

    @Override
    public int compareTo(@NonNull ParsedHeaderLine o) {

        // MUST be consistent with equals() - if equals(o) is true, compareTo(o) must be 0.
        int directiveComparison = Integer.compare(o.getHeaderDirective().getCardinality(), this.headerDirective.getCardinality()); // Still reverse order

        if (directiveComparison != 0) {
            return directiveComparison;
        }

        // headerDirectives are equal (by cardinality), compare the values (trimmed)
        return this.value.trim().compareTo(o.value.trim());
    }
}
