package com.pourchoices.chordpro.adapter.out.file;

import com.pourchoices.chordpro.application.domain.model.SetlistEntry;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Renders the floor setlist as one self-contained HTML document: landscape Letter, one big
 * SET / NAME / (file stem) / BACKING row per song, lots of white space, and a small footer saying
 * which sheet you're holding. The name is the standard 12-character RC-500 SONG LABEL, so every
 * row is about the same width and the type can be huge; a song without a label falls back to its
 * title. The stem is the end of the song ID — the file name without path or
 * extension — shown under the name so you can double-check you've got the right file.
 *
 * <p>Every row has the same height (the page's usable height divided by the fullest page's
 * row count, capped) and the type is a fixed fraction of that, so every sheet looks alike.
 * A name that can't fit on one line at full size is fitted, just that row, by a few lines of
 * inline script on load: shrunk on one line or wrapped to two, whichever leaves bigger type.
 * Everything else stays as big as the row allows.
 */
@Component
public class SetlistSheetHtmlRenderer {

    /** Landscape Letter, 0.4in margins → 7.7in usable; leave slack so no blank spill-over page. */
    private static final double SHEET_HEIGHT_INCHES = 7.5;
    /**
     * Fixed so the fit script measures the same layout that prints, whatever the browser window
     * size is (10.2in usable, minus slack).
     */
    private static final double SHEET_WIDTH_INCHES = 10.1;
    private static final double FOOTER_HEIGHT_INCHES = 0.35;
    /** Without a cap a one-page, three-song setlist would render absurdly tall rows. */
    private static final double MAX_ROW_HEIGHT_INCHES = 1.5;

    public String render(String gig, List<List<SetlistEntry>> pages) {
        int tallestPage = pages.stream().mapToInt(List::size).max().orElse(1);
        double rowHeight = Math.min(MAX_ROW_HEIGHT_INCHES,
                (SHEET_HEIGHT_INCHES - FOOTER_HEIGHT_INCHES) / tallestPage);

        StringBuilder html = new StringBuilder(head(gig, rowHeight));
        for (int i = 0; i < pages.size(); i++) {
            html.append(page(gig, i + 1, pages.size(), pages.get(i)));
        }
        return html.append(fitScript()).append("</body>\n</html>\n").toString();
    }

    private String head(String gig, double rowHeight) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                <meta charset="utf-8">
                <title>%s floor setlist</title>
                <style>
                  @page { size: letter landscape; margin: 0.4in; }
                  :root { --row-h: %sin; }
                  body { margin: 0; font-family: Helvetica, Arial, sans-serif; color: #000; }
                  .page { width: %sin; height: %sin; display: flex; flex-direction: column;
                          break-after: page; page-break-after: always; }
                  .page:last-child { break-after: auto; page-break-after: auto; }
                  .rows { flex: 1; }
                  .row { display: grid; grid-template-columns: 1.9in 1fr 1.5in; column-gap: 0.25in;
                         align-items: center;
                         height: var(--row-h); border-bottom: 2px solid #999;
                         font-size: calc(var(--row-h) * 0.58); line-height: 1; }
                  .row > * { min-width: 0; white-space: nowrap; }
                  .set { font-weight: bold; padding-left: 0.1in; }
                  .title { overflow: hidden; height: var(--row-h); box-sizing: border-box;
                           padding: 0.05in 0; display: grid; align-content: center; }
                  .name { font-size: calc(var(--row-h) * 0.5); line-height: 1.15; }
                  .name.label { white-space: pre; }
                  .name.fallback { font-style: italic; color: #666; }
                  .id { font-size: calc(var(--row-h) * 0.22); line-height: 1.2; color: #444; }
                  .backing { font-weight: bold; text-align: center; }
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
                .append("<span class=\"set\">").append(escape(entry.getSet())).append("</span>")
                .append("<span class=\"title\">").append(name(entry))
                .append("<span class=\"id\">(").append(escape(entry.getSongId().getFileStem()))
                .append(")</span></span>")
                .append("<span class=\"backing\">").append(escape(entry.getBackingLabel())).append("</span>")
                .append("</div>\n");
        }
        char setLetter = entries.get(0).getSet().charAt(0);
        return page.append("</div>\n<div class=\"foot\">")
                .append(escape(gig)).append(" &middot; Set ").append(setLetter)
                .append(" &middot; sheet ").append(sheet).append(" of ").append(sheetCount)
                .append("</div>\n</section>\n").toString();
    }

    /**
     * The big line: the RC-500 SONG LABEL exactly as the device shows it (padding spaces
     * preserved), or, when no label has been written yet, the full title in italic grey so a
     * glance tells a real device label from a stand-in.
     */
    private String name(SetlistEntry entry) {
        if (entry.hasSongLabel()) {
            return "<span class=\"name label\">" + escape(entry.getSongLabel()) + "</span>";
        }
        return "<span class=\"name fallback\">" + escape(entry.getTitle()) + "</span>";
    }

    /**
     * Song names that overflow their cell get the larger of two fits: one line shrunk to width,
     * or two lines shrunk to width and row height. Names that already fit stay at full size.
     * The file-stem line under the name is never touched.
     */
    private String fitScript() {
        return """
                <script>
                  function overflows(cell) {
                    return cell.scrollWidth > cell.clientWidth || cell.scrollHeight > cell.clientHeight;
                  }
                  function shrinkToFit(cell, name, start) {
                    var size = start;
                    name.style.fontSize = size + 'px';
                    while (overflows(cell) && size > 16) {
                      size -= 1;
                      name.style.fontSize = size + 'px';
                    }
                    return size;
                  }
                  document.querySelectorAll('.title').forEach(function (cell) {
                    if (!overflows(cell)) return;
                    var name = cell.querySelector('.name');
                    var start = parseFloat(getComputedStyle(name).fontSize);
                    var oneLine = shrinkToFit(cell, name, start);
                    name.style.whiteSpace = 'normal';
                    name.style.textWrap = 'balance';
                    name.style.lineHeight = '1.05';
                    var twoLines = shrinkToFit(cell, name, start);
                    if (oneLine >= twoLines) {
                      name.style.whiteSpace = '';
                      name.style.textWrap = '';
                      name.style.lineHeight = '';
                      name.style.fontSize = oneLine + 'px';
                    }
                  });
                </script>
                """;
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
