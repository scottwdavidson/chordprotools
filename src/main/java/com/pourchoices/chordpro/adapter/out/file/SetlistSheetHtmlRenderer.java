package com.pourchoices.chordpro.adapter.out.file;

import com.pourchoices.chordpro.application.domain.model.SetlistEntry;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

import static com.pourchoices.chordpro.adapter.out.file.Html.escape;

/**
 * Renders the floor setlist as one self-contained HTML document: portrait Letter, one big
 * {@code A01-45 [guitar] Song Title} row per song (set code, a dash and the backing slot —
 * {@code BB} when there's no RC-500 track, as a reminder to cue the BeatBuddy — then a guitar
 * icon for the guitarist's instrument, then the title), and a small footer saying which sheet
 * you're holding.
 *
 * <p>One sheet per set — the caller decides what goes on each page. Every row has the same
 * height — the page's usable height divided by the standard sheet's row count, or by the
 * fullest sheet's if that's bigger — and the type is a fixed fraction of it, so every gig
 * prints with the same look. A title too long for its single line is simply cut off with an
 * ellipsis.
 */
@Component
public class SetlistSheetHtmlRenderer {

    /** Rows a standard sheet is sized for; a set with more songs shrinks every row to fit. */
    static final int STANDARD_SHEET_ROWS = 16;
    /** Portrait Letter, 0.5in margins → 10in usable; leave slack so no blank spill-over page. */
    private static final double SHEET_HEIGHT_INCHES = 9.8;
    /** 7.5in usable, minus slack. */
    private static final double SHEET_WIDTH_INCHES = 7.4;
    private static final double FOOTER_HEIGHT_INCHES = 0.3;
    private static final String BEAT_BUDDY = "BB";

    public String render(String gig, List<List<SetlistEntry>> pages) {
        int tallestPage = pages.stream().mapToInt(List::size).max().orElse(0);
        int rowsToFit = Math.max(tallestPage, STANDARD_SHEET_ROWS);
        double rowHeight = (SHEET_HEIGHT_INCHES - FOOTER_HEIGHT_INCHES) / rowsToFit;

        StringBuilder html = new StringBuilder(head(gig, rowHeight));
        html.append(GuitarIcons.definitions());
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
                  .row { display: grid; grid-template-columns: 3.9em calc(var(--row-h) * 0.55) 1fr; column-gap: 0.3em;
                         align-items: center; box-sizing: border-box; height: var(--row-h);
                         border-bottom: 1px solid #999;
                         font-size: calc(var(--row-h) * 0.62); line-height: 1.3; }
                  .code { font-weight: bold; white-space: nowrap; }
                  .guitar { display: flex; justify-content: center; align-items: center; }
                  .guitar-icon { height: calc(var(--row-h) * 0.86); width: auto; fill: #000; } /* icon is 28:44, so ~0.55 row-h wide */
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
        for (SetlistEntry entry : entries) {
            page.append("<div class=\"row\">")
                .append("<span class=\"code\">").append(escape(code(entry))).append("</span>")
                .append("<span class=\"guitar\">").append(GuitarIcons.icon(entry.getGuitarType())).append("</span>")
                .append("<span class=\"title\">").append(escape(entry.getTitle())).append("</span>")
                .append("</div>\n");
        }
        return page.append("</div>\n<div class=\"foot\">")
                .append(escape(gig)).append(" &middot; sheet ").append(sheet).append(" of ").append(sheetCount)
                .append("</div>\n</section>\n").toString();
    }

    /**
     * Compact set + backing, e.g. {@code A01-45}, {@code B09-BB}. No RC-500 track means the
     * BeatBuddy has to be cued, so an empty backing shows as {@code BB} rather than nothing.
     */
    private String code(SetlistEntry entry) {
        String backing = entry.getBackingLabel();
        return entry.getSet() + "-" + (backing.isEmpty() ? BEAT_BUDDY : backing);
    }

    private String inches(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }
}
