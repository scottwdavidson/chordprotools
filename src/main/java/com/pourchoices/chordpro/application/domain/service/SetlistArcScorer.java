package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.ArcThird;
import com.pourchoices.chordpro.application.domain.model.SetlistEntry;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Pure energy-arc / sing-along-placement scoring logic for
 * {@code evaluate-setlist}. No I/O — takes an already-ordered, already
 * de-duplicated {@link SetlistEntry} list (fan-facing sets only; Z-set
 * backup songs are the caller's responsibility to filter out first, since
 * they're not part of the performed arc).
 *
 * <p>Splits into thirds by count, not by set letter — a venue's "shape"
 * concern is about energy over time, not about how the sets happen to be
 * labeled.
 */
@Component
public class SetlistArcScorer {

    /** How many songs at the tail of the set count as "closers" for the sing-along check. */
    public static final int DEFAULT_CLOSER_WINDOW = 3;

    /**
     * @return {@code [opening, middle, final]} raw sublists. Any third may be
     *     empty if there are fewer than 3 songs total.
     */
    public List<List<SetlistEntry>> splitIntoThirds(List<SetlistEntry> orderedEntries) {
        int n = orderedEntries.size();
        int thirdSize = n / 3;
        int firstEnd = thirdSize;
        int secondEnd = thirdSize * 2;
        return List.of(
                orderedEntries.subList(0, firstEnd),
                orderedEntries.subList(firstEnd, secondEnd),
                orderedEntries.subList(secondEnd, n)
        );
    }

    /** @return per-third energy summaries, or an empty list if {@code orderedEntries} is empty. */
    public List<ArcThird> computeThirds(List<SetlistEntry> orderedEntries) {
        if (orderedEntries.isEmpty()) return List.of();

        String[] labels = {"Opening", "Middle", "Final"};
        List<List<SetlistEntry>> thirds = splitIntoThirds(orderedEntries);

        List<ArcThird> result = new ArrayList<>(3);
        for (int i = 0; i < 3; i++) {
            result.add(summarize(labels[i], thirds.get(i)));
        }
        return result;
    }

    /** Total songs across the whole setlist marked {@code singAlong == true}. */
    public int singAlongCount(List<SetlistEntry> entries) {
        return (int) entries.stream()
                .filter(e -> Boolean.TRUE.equals(e.getSong().getSingAlong()))
                .count();
    }

    /** Sing-along count restricted to the final third only. */
    public int singAlongCountInFinalThird(List<SetlistEntry> orderedEntries) {
        if (orderedEntries.isEmpty()) return 0;
        List<SetlistEntry> finalThird = splitIntoThirds(orderedEntries).get(2);
        return singAlongCount(finalThird);
    }

    /** @return true if any of the last {@code windowSize} songs is a sing-along. */
    public boolean lastSongsIncludeSingAlong(List<SetlistEntry> orderedEntries, int windowSize) {
        if (orderedEntries.isEmpty()) return false;
        int from = Math.max(0, orderedEntries.size() - windowSize);
        return singAlongCount(orderedEntries.subList(from, orderedEntries.size())) > 0;
    }

    private ArcThird summarize(String label, List<SetlistEntry> entries) {
        List<Integer> energies = entries.stream()
                .map(e -> e.getSong().getEnergyLevel())
                .filter(Objects::nonNull)
                .toList();

        Double avg = energies.isEmpty() ? null
                : energies.stream().mapToInt(Integer::intValue).average().orElse(0);
        Double median = energies.isEmpty() ? null : median(energies);

        return ArcThird.builder()
                .label(label)
                .songCount(entries.size())
                .characterizedCount(energies.size())
                .avgEnergy(avg)
                .medianEnergy(median)
                .build();
    }

    private double median(List<Integer> values) {
        List<Integer> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int size = sorted.size();
        if (size % 2 == 1) return sorted.get(size / 2);
        return (sorted.get(size / 2 - 1) + sorted.get(size / 2)) / 2.0;
    }
}
