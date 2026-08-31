package com.pourchoices.chordpro.application.domain.model;

import lombok.Value;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects and repairs the one well-understood way a {@code gigs.csv} row
 * goes bad after a spreadsheet round-trip: a fully-blank trailing cell gets
 * silently dropped on save, leaving a data row with fewer comma-separated
 * fields than the header.
 *
 * <p><b>Only safe for {@code gigs.csv}.</b> This does a naive comma split -
 * correct only because none of its columns (GIG, SONG ID, SET, RC SLOT) are
 * ever expected to contain a literal comma. Do not reuse this for a
 * free-text CSV like {@code song-catalog.csv}, where a comma inside a
 * quoted title would make a naive split miscount fields.
 *
 * <p>Repair strategy, deliberately narrow:
 * <ul>
 *   <li>Row has exactly the header's field count - left untouched.</li>
 *   <li>Row has fewer fields than the header - assumed to be missing
 *       trailing blank cells; padded with empty trailing fields.</li>
 *   <li>Row has more fields than the header - NOT guessed at; flagged for
 *       human review instead. Could be a genuine unescaped comma or a
 *       different kind of corruption - safer to refuse than to guess.</li>
 *   <li>Blank lines are left untouched and never flagged either way.</li>
 * </ul>
 *
 * <p>The expected field count is read from the file's own header row (index
 * 0), not hard-coded anywhere - if a column is ever added to
 * {@code gigs.csv}, this self-adapts without a code change.
 */
public final class GigsRowRepair {

    private GigsRowRepair() {}

    public static Report repair(List<String> rawLines) {
        if (rawLines.isEmpty()) {
            return new Report(rawLines, List.of(), List.of());
        }

        int expectedFieldCount = rawLines.get(0).split(",", -1).length;
        List<String> repairedLines = new ArrayList<>(rawLines);
        List<Integer> repairedLineNumbers = new ArrayList<>();
        List<RejectedRow> rejectedRows = new ArrayList<>();

        for (int i = 1; i < rawLines.size(); i++) {
            String line = rawLines.get(i);
            if (line.isBlank()) {
                continue;
            }

            int actualFieldCount = line.split(",", -1).length;
            int lineNumber = i + 1; // 1-based, matches what a human sees in an editor

            if (actualFieldCount == expectedFieldCount) {
                continue;
            } else if (actualFieldCount < expectedFieldCount) {
                int missingFields = expectedFieldCount - actualFieldCount;
                repairedLines.set(i, line + ",".repeat(missingFields));
                repairedLineNumbers.add(lineNumber);
            } else {
                rejectedRows.add(new RejectedRow(lineNumber, line));
            }
        }

        return new Report(repairedLines, repairedLineNumbers, rejectedRows);
    }

    @Value
    public static class Report {
        List<String> repairedLines;
        List<Integer> repairedLineNumbers;
        List<RejectedRow> rejectedRows;

        public boolean hasRepairs() {
            return !repairedLineNumbers.isEmpty();
        }

        public boolean hasRejections() {
            return !rejectedRows.isEmpty();
        }
    }

    @Value
    public static class RejectedRow {
        int lineNumber;
        String rawContent;
    }
}
