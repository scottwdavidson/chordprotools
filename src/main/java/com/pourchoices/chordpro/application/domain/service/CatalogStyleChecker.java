package com.pourchoices.chordpro.application.domain.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * House-style checks for catalog titles and artists - the guardrail that keeps the data
 * consistent now that the artist is the search key and the titles print on the fans' handout.
 *
 * <p>The rules, all advisory (they warn, they never block):
 * <ul>
 *   <li><b>Titles</b> use initial caps on every word ("Year Of The Cat") and straight quotes.</li>
 *   <li><b>Artists</b> are the primary artist only - no "A / B", "A (B)" or "A feat. B" credits.</li>
 *   <li><b>One spelling per act</b> - "Cold Play" next to "Coldplay" is flagged, so a typo can't
 *       quietly split one artist into two.</li>
 * </ul>
 *
 * <p>Pure functions over strings: no I/O, no printing. Callers decide how to present the warnings.
 */
@Component
public class CatalogStyleChecker {

    /** A lowercase letter that starts a word: at the start, or after space, "(", "/", "-" or a quote. */
    private static final Pattern LOWERCASE_WORD_START = Pattern.compile("(^|[\\s(/\\-\"])(\\p{Ll}\\S*)");
    private static final Pattern CURLY_QUOTE = Pattern.compile("[\u2018\u2019\u201c\u201d]");
    private static final Pattern COLLABORATION_CREDIT =
            Pattern.compile("/|\\(|\\bfeat\\b|\\bfeaturing\\b|\\bw/", Pattern.CASE_INSENSITIVE);
    private static final Pattern LEADING_THE = Pattern.compile("^the");
    private static final Pattern NOT_ALPHANUMERIC = Pattern.compile("[^a-z0-9]");

    /** Names shorter than this are too short to call "one typo apart" without false alarms. */
    private static final int MIN_LENGTH_FOR_TYPO_MATCH = 6;

    public List<String> titleWarnings(String title) {
        List<String> warnings = new ArrayList<>();
        if (title == null) return warnings;
        Matcher lowercase = LOWERCASE_WORD_START.matcher(title);
        if (lowercase.find()) {
            warnings.add("title not initial caps: '" + lowercase.group(2) + "' should start with a capital");
        }
        if (CURLY_QUOTE.matcher(title).find()) {
            warnings.add("title has a curly quote; use a straight ' or \"");
        }
        return warnings;
    }

    public List<String> artistWarnings(String artist) {
        List<String> warnings = new ArrayList<>();
        if (artist != null && COLLABORATION_CREDIT.matcher(artist).find()) {
            warnings.add("artist looks like a collaboration credit; use the primary artist only");
        }
        return warnings;
    }

    /** An already-known artist spelled almost the same way (but not identically), if there is one. */
    public Optional<String> similarArtist(String artist, Collection<String> knownArtists) {
        return knownArtists.stream()
                .filter(known -> !known.equals(artist) && looksLikeSameArtist(artist, known))
                .sorted()
                .findFirst();
    }

    /** Every pair of distinct artist spellings that look like the same act, each pair listed once. */
    public List<String[]> alikeArtistPairs(Collection<String> artists) {
        List<String> distinct = artists.stream().distinct().sorted().toList();
        List<String[]> pairs = new ArrayList<>();
        for (int i = 0; i < distinct.size(); i++) {
            for (int j = i + 1; j < distinct.size(); j++) {
                if (looksLikeSameArtist(distinct.get(i), distinct.get(j))) {
                    pairs.add(new String[]{distinct.get(i), distinct.get(j)});
                }
            }
        }
        return pairs;
    }

    private boolean looksLikeSameArtist(String a, String b) {
        String x = normalize(a);
        String y = normalize(b);
        if (x.equals(y)) return true;
        return x.length() >= MIN_LENGTH_FOR_TYPO_MATCH && y.length() >= MIN_LENGTH_FOR_TYPO_MATCH
                && withinOneEdit(x, y);
    }

    /** Lowercase, "&" as "and", no leading "The", letters and digits only. */
    private String normalize(String artist) {
        String lower = artist.toLowerCase(Locale.ROOT).replace("&", "and");
        lower = LEADING_THE.matcher(lower.strip()).replaceFirst("");
        return NOT_ALPHANUMERIC.matcher(lower).replaceAll("");
    }

    /** True when the strings differ by at most one inserted, deleted or substituted character. */
    private boolean withinOneEdit(String x, String y) {
        if (Math.abs(x.length() - y.length()) > 1) return false;
        String shorter = x.length() <= y.length() ? x : y;
        String longer = x.length() <= y.length() ? y : x;
        int i = 0;
        while (i < shorter.length() && shorter.charAt(i) == longer.charAt(i)) i++;
        if (i == shorter.length()) return true;                       // one extra char at the end
        if (shorter.length() == longer.length()) {
            return shorter.substring(i + 1).equals(longer.substring(i + 1));   // substitution
        }
        return shorter.substring(i).equals(longer.substring(i + 1));          // insertion / deletion
    }
}
