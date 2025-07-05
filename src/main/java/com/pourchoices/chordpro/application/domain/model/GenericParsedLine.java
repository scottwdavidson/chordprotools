package com.pourchoices.chordpro.application.domain.model;

import com.pourchoices.chordpro.application.domain.model.HeaderDirective;
import com.pourchoices.chordpro.application.domain.model.ParsedHeaderLine;
import com.pourchoices.chordpro.application.domain.model.ParsedSongPhrase;
import com.pourchoices.chordpro.application.domain.model.SongDirective;
import lombok.Builder;
import lombok.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


@Value
@Builder
public class GenericParsedLine {
    String directive;
    String value;

    public static GenericParsedLine.GenericParsedLineBuilder from(String line) {

        GenericParsedLineBuilder builder = new GenericParsedLineBuilder();

        // remove leading & trailing blanks
        String cleanedLine = line.trim();

        // extract the first "directive" up to the colon or equals mark delimiter
        String[] directiveValue = cleanedLine.split("[:=]", 2);

        // directive
        builder.directive(directiveValue[0]);

        // value if there is one
        if (directiveValue.length > 1) {
            builder.value(directiveValue[1].trim());
        }

        return builder;
    }
}

