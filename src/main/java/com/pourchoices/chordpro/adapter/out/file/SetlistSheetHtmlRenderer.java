package com.pourchoices.chordpro.adapter.out.file;

import com.pourchoices.chordpro.application.domain.model.SetlistEntry;
import com.pourchoices.chordpro.application.domain.service.SetlistPageSplitter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Renders the floor setlist as one self-contained HTML document: portrait Letter, one big
 * {@code A01-45 Song Title} row per song (set code, a dash and the backing slot, then the
 * title), and a small footer saying which sheet you're holding.
 *
 * <p>Every row has the same height — the page's usable height divided by the standard sheet's
 * row count, or by the fullest sheet's if that's bigger — and the type is a fixed fraction of
 * it, so every gig prints with the same look. A title too long for its single line is simply
 * cut off with an ellipsis. A heavy rule marks where a new set starts.
 */
@Component
public class SetlistSheetHtmlRenderer {

    /** Portrait Letter, 0.5in margins → 10in usable; leave slack so no blank spill-over page. */
    private static final double SHEET_HEIGHT_INCHES = 9.8;
    /** 7.5in usable, minus slack. */
    private static final double SHEET_WIDTH_INCHES = 7.4;
    private static final double FOOTER_HEIGHT_INCHES = 0.3;

    public String render(String gig, List<List<SetlistEntry>> pages) {
        int tallestPage = pages.stream().mapToInt(List::size).max().orElse(0);
        int rowsToFit = Math.max(tallestPage, SetlistPageSplitter.SONGS_PER_SHEET);
        double rowHeight = (SHEET_HEIGHT_INCHES - FOOTER_HEIGHT_INCHES) / rowsToFit;

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
                <title>%s floor setlist</title>
                <style>
                  @page { size: letter portrait; margin: 0.5in; }
                  :root { --row-h: %sin; }
                  body { margin: 0; font-family: Helvetica, Arial, sans-serif; color: #000; }
                  .page { width: %sin; height: %sin; display: flex; flex-direction: column;
                          break-after: page; page-break-after: always; }
                  .page:last-child { break-after: auto; page-break-after: auto; }
                  .rows { flex: 1; }
                  .row { display: grid; grid-template-columns: 3.9em 1fr; column-gap: 0.4em;
                         align-items: center; box-sizing: border-box; height: var(--row-h);
                         border-bottom: 1px solid #999;
                         font-size: calc(var(--row-h) * 0.62); line-height: 1.3; }
                  .row.new-set { border-top: 3px solid #000; }
                  .code { font-weight: bold; white-space: nowrap; }
                  .title { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; min-width: 0; }
                  .foot { height: %sin; font-size: 12pt; color: #555; text-align: right; }
                </style>
                </head>
                <body>
                """.formatted(escape(gig), inches(rowHeight), inches(SHEET_WIDTH_INCHES),
                inches(SHEET_HEIGHT_INCHES), inches(FOOTER_HEIGHT_INCHES));
    }

    private String page(String gig, int sheet, int sheetCount, List<SetlistEntry> entries) {
        StringBuilder page = new StringBuilder("<section class=\"page\">\n<div class=\"rows\">\n");
        char previousSet = 0;
        for (SetlistEntry entry : entries) {
            char set = entry.getSet().charAt(0);
            String rowClass = (previousSet != 0 && set != previousSet) ? "row new-set" : "row";
            page.append("<div class=\"").append(rowClass).append("\">")
                .append("<span class=\"code\">").append(escape(code(entry))).append("</span>")
                .append("<span class=\"title\">").append(escape(entry.getTitle())).append("</span>")
                .append("</div>\n");
            previousSet = set;
        }
        return page.append("</div>\n<div class=\"foot\">")
                .append(escape(gig)).append(" &middot; sheet ").append(sheet).append(" of ").append(sheetCount)
                .append("</div>\n</section>\n").toString();
    }

    /** Compact set + backing, e.g. {@code A01-45}, {@code B09-BB}; just {@code A01} when there is no backing. */
    private String code(SetlistEntry entry) {
        String backing = entry.getBackingLabel();
        return backing.isEmpty() ? entry.getSet() : entry.getSet() + "-" + backing;
    }

    private String inches(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private String escape(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
