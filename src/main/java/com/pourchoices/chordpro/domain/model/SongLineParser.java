package com.pourchoices.chordpro.domain.model;

import lombok.Builder;
import lombok.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


@Service
public class SongLineParser {

    private final static Logger LOGGER = LoggerFactory.getLogger(SongLineParser.class);

    public ParsedHeaderLine parseHeaderLine(String line) {

        // remove beginning and ending curly braces
        String cleanedLine = line.replaceAll("^\\{|\\}$", "");

        // remove beginning and ending white space
        cleanedLine = cleanedLine.trim();

        // extract a general parsed line for evaluation
        GenericParsedLine genericParsedLine = GenericParsedLine.from(cleanedLine).build();

        HeaderDirective headerDirective = HeaderDirective.getByPrefix(genericParsedLine.getDirective());
        SongDirective songDirective = SongDirective.getByPrefix(genericParsedLine.getDirective());
        if (null == headerDirective && null == songDirective) {
            return null;
        } else if (headerDirective == HeaderDirective.UNPARSED_META) {

            // need to extract the meta directive
            cleanedLine = removeInitialDirective(cleanedLine);

            // extract a general parsed line for evaluation
            genericParsedLine = GenericParsedLine.from(cleanedLine).build();
            String metaDirectivePrefix = genericParsedLine.getDirective().trim();

            headerDirective = HeaderDirective.getByPrefix(metaDirectivePrefix);

            if (null == headerDirective) {
                return null;
            } else {
                return ParsedHeaderLine.builder()
                        .headerDirective(headerDirective)
                        .value(genericParsedLine.getValue().trim())
                        .build();
            }
        } else if (headerDirective != null) {
            return ParsedHeaderLine.builder()
                    .headerDirective(headerDirective)
                    .value(genericParsedLine.getValue())
                    .build();
        } else if (songDirective == SongDirective.SONG_COMMENT) {
            
            // need to extract the comment song directive
            cleanedLine = removeInitialDirective(cleanedLine);

            // check for ephemeral comment
            if (cleanedLine.startsWith("**")) {
                return ParsedHeaderLine.builder()
                        .headerDirective(HeaderDirective.EPHEMERAL_COMMENT)
                        .value(cleanedLine)
                        .build();

            }

            // extract a general parsed line for evaluation
            genericParsedLine = GenericParsedLine.from(cleanedLine).build();
            String commentDirectivePrefix = genericParsedLine.getDirective().trim();

            HeaderDirective possibleHeaderDirective = HeaderDirective.getByPrefix(commentDirectivePrefix);

            if (null == possibleHeaderDirective) {
                return null;
            } else {
                return ParsedHeaderLine.builder()
                        .headerDirective(possibleHeaderDirective)
                        .value(genericParsedLine.getValue().trim())
                        .build();
            }

        } else {
            // no match
            return null;
        }

    }

    public ParsedSongPhrase parseSongPhrase(String line) {

        String cleanedLine = line.trim();

        // if it's not a directive, return null
        if (!(cleanedLine.startsWith("{") && cleanedLine.endsWith("}"))) {
            return null;
        }

        // remove beginning and ending curly braces
        cleanedLine = line.replaceAll("^\\{|\\}$", "");

        // remove beginning and ending white space
        cleanedLine = cleanedLine.trim();

        // brute force: if it's a document comment since there's not tag
        if ((cleanedLine.startsWith("{") && cleanedLine.endsWith("}"))) {

            // remove beginning and ending curly braces
            cleanedLine = cleanedLine.replaceAll("^\\{|\\}$", "");

            return ParsedSongPhrase.builder()
                    .songDirective(SongDirective.DOCUMENT_COMMENT)
                    .line(cleanedLine)
                    .build();
        }

        // extract a general parsed line for evaluation
        GenericParsedLine genericParsedLine = GenericParsedLine.from(cleanedLine).build();

        SongDirective songDirective = SongDirective.getByPrefix(genericParsedLine.getDirective());
        if (null == songDirective) {
            return null;
        } else if (!songDirective.isSingleLineDirective()) {

            return ParsedSongPhrase.builder()
                    .songDirective(songDirective)
                    .build();
        } else {

            return ParsedSongPhrase.builder()
                    .songDirective(songDirective)
                    .line(genericParsedLine.getValue())
                    .build();
        }

    }

    // ex: input "meta: nord: P45", returns "nord: P45"
    private String removeInitialDirective(String line) {
        return line.substring(line.indexOf(":") + 1).trim();
    }

    @Value
    @Builder
    private static class GenericParsedLine {
        String directive;
        String value;

        public static GenericParsedLine.GenericParsedLineBuilder from(String line) {

            GenericParsedLine.GenericParsedLineBuilder builder = new GenericParsedLine.GenericParsedLineBuilder();

            // remove leading & trailing blanks
            String cleanedLine = line.trim();

            // extract the first "directive" up to the colon or equals mark delimiter
            String[] directiveValue = cleanedLine.split("[:=]", 2);

            // directive
            builder.directive = directiveValue[0];

            // value if there is one
            if (directiveValue.length > 1) {
                builder.value = directiveValue[1].trim();
            }

            return builder;
        }
    }
}
