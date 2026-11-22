package com.pourchoices.chordpro.adapter.out.file;

import com.pourchoices.chordpro.application.domain.model.SetlistEntry;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static com.pourchoices.chordpro.adapter.out.file.Html.escape;

/**
 * Renders the fans' setlist handout as one self-contained HTML document: portrait Letter, the
 * band name, a "venue &middot; date" line, then each set under a "Set N" heading with one
 * {@code Title ... Artist} row per song. No band-only noise (RC-500 slots, guitar icons).
 *
 * <p>The text simply flows: a short setlist fits one page, a full gig runs to about two. A song
 * row is never split, and a set heading is never left stranded at the bottom of a page.
 *
 * <p>The date shown is the one at the front of the gig slug (e.g. {@code 2026-11-20-MOODS});
 * a slug without a leading date just prints without one.
 */
@Component
public class FanSetlistHtmlRenderer {

    private static final String BAND_NAME = "Pour Choices";
    private static final int SLUG_DATE_LENGTH = "yyyy-MM-dd".length();
    private static final DateTimeFormatter LONG_DATE =
            DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.US);

    public String render(String gig, String venue, List<List<SetlistEntry>> sets) {
        StringBuilder html = new StringBuilder(head(gig));
        html.append("<header>\n<h1>").append(escape(BAND_NAME)).append("</h1>\n");
        String when = whenLine(gig, venue);
        if (!when.isEmpty()) html.append("<p class=\"when\">").append(escape(when)).append("</p>\n");
        html.append("</header>\n");
        for (int i = 0; i < sets.size(); i++) {
            html.append(set(i + 1, sets.get(i)));
        }
        return html.append("</body>\n</html>\n").toString();
    }

    private String head(String gig) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                <meta charset="utf-8">
                <title>%s setlist - %s</title>
                <style>
                  @page { size: letter portrait; margin: 0.75in; }
                  body { margin: 0; font-family: Helvetica, Arial, sans-serif; font-size: 13pt; color: #000; }
                  h1 { margin: 0; text-align: center; font-size: 30pt; letter-spacing: 0.04em; }
                  .when { margin: 4pt 0 0; text-align: center; font-size: 13pt; color: #333; }
                  .set { margin-top: 12pt; }
                  h2 { margin: 0 0 4pt; padding-bottom: 2pt; border-bottom: 2px solid #000;
                       font-size: 15pt; text-transform: uppercase; letter-spacing: 0.12em;
                       break-after: avoid; page-break-after: avoid; }
                  ul { margin: 0; padding: 0; list-style: none; }
                  li { display: flex; justify-content: space-between; gap: 1em; padding: 2pt 0;
                       border-bottom: 1px solid #ddd; line-height: 1.25;
                       break-inside: avoid; page-break-inside: avoid; }
                  .song { font-weight: bold; }
                  .artist { color: #444; text-align: right; }
                </style>
                </head>
                <body>
                """.formatted(escape(BAND_NAME), escape(gig));
    }

    private String set(int number, List<SetlistEntry> entries) {
        StringBuilder set = new StringBuilder("<section class=\"set\">\n<h2>Set ").append(number).append("</h2>\n<ul>\n");
        for (SetlistEntry entry : entries) {
            set.append("<li><span class=\"song\">").append(escape(entry.getTitle())).append("</span>")
               .append("<span class=\"artist\">").append(escape(entry.getArtist())).append("</span></li>\n");
        }
        return set.append("</ul>\n</section>\n").toString();
    }

    /** {@code "Moods Wine Bar · Friday, November 20, 2026"}, or whichever half is known, or empty. */
    private String whenLine(String gig, String venue) {
        String date = slugDate(gig);
        return Stream.of(venue, date)
                .filter(part -> part != null && !part.isBlank())
                .reduce((venuePart, datePart) -> venuePart + " \u00b7 " + datePart)
                .orElse("");
    }

    private String slugDate(String gig) {
        if (gig == null || gig.length() < SLUG_DATE_LENGTH) return null;
        try {
            return LocalDate.parse(gig.substring(0, SLUG_DATE_LENGTH)).format(LONG_DATE);
        } catch (DateTimeParseException notADate) {
            return null;
        }
    }
}
