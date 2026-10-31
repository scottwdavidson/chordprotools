package com.pourchoices.chordpro.adapter.out.file;

import com.pourchoices.chordpro.application.domain.model.SetlistEntry;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Renders the stage-ready setlist as one self-contained HTML document: a big, high-contrast
 * SET / TITLE / BACKING table per printed page.
 *
 * <p>Open it in a browser and print — the embedded {@code @page} rule asks for portrait
 * Letter with half-inch margins, and each page is forced onto its own sheet. Row height is
 * derived from the fullest page so every page is guaranteed to fit on one sheet, using the
 * biggest type that allows (capped so short setlists don't look silly).
 */
@Component
public class SetlistSheetHtmlRenderer {

    /** Vertical space for the table on a Letter page: 11in − 2×0.5in margins − heading − safety. */
    private static final double TABLE_HEIGHT_INCHES = 9.3;
    private static final double MAX_ROW_HEIGHT_INCHES = 0.42;

    public String render(String gig, List<List<SetlistEntry>> pages) {
        int tallestPage = pages.stream().mapToInt(List::size).max().orElse(0);
        int rowsToFit = tallestPage + 1; // + column header row
        double rowHeight = Math.min(MAX_ROW_HEIGHT_INCHES, TABLE_HEIGHT_INCHES / rowsToFit);

        StringBuilder html = new StringBuilder(head(gig, rowHeight));
        for (int i = 0; i < pages.size(); i++) {
            html.append(page(gig, i + 1, pages.size(), pages.get(i)));
        }
        return html.append("</body>\n</html>\n").toString();
    }

    private String head(String gig, double rowHeight) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                <meta charset="utf-8">
                <title>%s setlist</title>
                <style>
                  @page { size: letter portrait; margin: 0.5in; }
                  :root { --row-h: %sin; }
                  body { margin: 0; font-family: Helvetica, Arial, sans-serif; color: #000; }
                  .page { break-after: page; page-break-after: always; }
                  .page:last-child { break-after: auto; page-break-after: auto; }
                  h1 { margin: 0 0 0.1in; height: 0.3in; font-size: 14pt; line-height: 0.3in; }
                  table { width: 100%%; border-collapse: collapse; table-layout: fixed; }
                  tr { height: var(--row-h); }
                  th, td { padding: 0 0.08in; overflow: hidden; white-space: nowrap; line-height: 1;
                           text-overflow: ellipsis; border-bottom: 1px solid #888;
                           font-size: calc(var(--row-h) * 0.62); }
                  th { text-align: left; font-size: calc(var(--row-h) * 0.45); letter-spacing: 0.05em;
                       border-bottom: 2px solid #000; }
                  .set { width: 0.9in; font-weight: bold; }
                  .backing { width: 1.1in; text-align: center; font-weight: bold; }
                  tr.new-set td { border-top: 2px solid #000; }
                </style>
                </head>
                <body>
                """.formatted(escape(gig), String.format(Locale.ROOT, "%.3f", rowHeight));
    }

    private String page(String gig, int pageNumber, int pageCount, List<SetlistEntry> entries) {
        StringBuilder page = new StringBuilder();
        page.append("<section class=\"page\">\n")
            .append("<h1>").append(escape(gig))
            .append(" &mdash; page ").append(pageNumber).append(" of ").append(pageCount).append("</h1>\n")
            .append("<table>\n<tr><th class=\"set\">SET</th><th>TITLE</th><th class=\"backing\">BACKING</th></tr>\n");

        char previousSet = 0;
        for (SetlistEntry entry : entries) {
            char set = entry.getSet().charAt(0);
            String rowClass = (previousSet != 0 && set != previousSet) ? " class=\"new-set\"" : "";
            page.append("<tr").append(rowClass).append(">")
                .append("<td class=\"set\">").append(escape(entry.getSet())).append("</td>")
                .append("<td>").append(escape(entry.getTitle())).append("</td>")
                .append("<td class=\"backing\">").append(escape(entry.getBackingLabel())).append("</td>")
                .append("</tr>\n");
            previousSet = set;
        }
        return page.append("</table>\n</section>\n").toString();
    }

    private String escape(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
