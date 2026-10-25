package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import com.pourchoices.chordpro.application.domain.model.MetadataConsistencyReport;
import com.pourchoices.chordpro.application.domain.model.MetadataConsistencyReport.Finding;
import com.pourchoices.chordpro.application.domain.model.MetadataConsistencyReport.FindingType;
import com.pourchoices.chordpro.application.domain.model.MusicalKey;
import com.pourchoices.chordpro.application.domain.model.SongId;
import com.pourchoices.chordpro.application.port.in.ConsistentMetadataUseCase;
import com.pourchoices.chordpro.application.port.out.CatalogPort;
import com.pourchoices.chordpro.config.ChordproCatalogIndexPathConfig;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implements {@link ConsistentMetadataUseCase}.
 *
 * <p>Groups catalog entries by {@link SongId#toGroupKey()} (same pattern as
 * {@link FindSongIdService}) and runs two checks per group, reusing
 * {@link CatalogEntryComparator} for the field-by-field drift comparison.
 */
@Service
@AllArgsConstructor(onConstructor_ = @__(@Autowired))
@Slf4j
public class ConsistentMetadataService implements ConsistentMetadataUseCase {

    private final CatalogPort catalogPort;
    private final ChordproCatalogIndexPathConfig catalogConfig;
    private final CatalogEntryComparator comparator;

    @Override
    public MetadataConsistencyReport check(boolean fix, String sourceSongId) {

        Path catalogPath = Paths.get(catalogConfig.getCatalogIndexPath());
        // CatalogPort returns an unmodifiable, order-preserving map -- copy into a
        // mutable LinkedHashMap (preserves that same order) since propagate() and
        // applyInferredPerformanceKey() below write into it directly.
        Map<String, CatalogEntry> catalog = new LinkedHashMap<>(catalogPort.readCatalogFromCsv(catalogPath));

        // ── Bucket entries by group key, preserving catalog order ────────────
        Map<String, List<CatalogEntry>> groups = new LinkedHashMap<>();
        for (CatalogEntry entry : catalog.values()) {
            groups.computeIfAbsent(entry.getSongId().toGroupKey(), k -> new ArrayList<>())
                    .add(entry);
        }

        List<Finding> findings = new ArrayList<>();
        int groupsChecked = 0;
        int consistentGroups = 0;
        boolean catalogMutated = false;

        for (Map.Entry<String, List<CatalogEntry>> e : groups.entrySet()) {
            String groupKey = e.getKey();
            List<CatalogEntry> variants = e.getValue();

            int findingsBefore = findings.size();

            // ── Check B: filename key vs {key:} — applies to every variant ───
            for (CatalogEntry variant : variants) {
                findFilenameKeyMismatch(variant).ifPresent(findings::add);
            }

            // ── Check A: cross-variant metadata drift — needs 2+ variants ────
            if (variants.size() >= 2) {
                groupsChecked++;
                CatalogEntry source = chooseSource(variants, sourceSongId);
                List<Finding> driftFindings = findDrift(groupKey, variants, source);
                findings.addAll(driftFindings);
                findMissingPerformanceKey(groupKey, variants, sourceSongId).ifPresent(findings::add);

                if (fix) {
                    if (!driftFindings.isEmpty()) {
                        if (source.getSongId().isBaseVersion()
                                || matchesNamedSource(source, sourceSongId)) {
                            propagate(source, variants, catalog);
                            catalogMutated = true;
                        } else {
                            log.warn("Skipping --fix for orphan group '{}' (no base variant; "
                                    + "pass --source <songId>)", groupKey);
                        }
                    }
                    Optional<String> inferredPerformanceKey = resolveInferredPerformanceKey(variants, sourceSongId);
                    if (inferredPerformanceKey.isPresent()) {
                        applyInferredPerformanceKey(inferredPerformanceKey.get(), variants, catalog);
                        catalogMutated = true;
                    }
                }
            }

            if (findings.size() == findingsBefore) {
                consistentGroups++;
            }
        }

        if (fix && catalogMutated) {
            catalogPort.writeCatalogToCsv(catalogPath, new ArrayList<>(catalog.values()));
            log.info("consistent-metadata --fix: catalog rewritten");
        }

        return MetadataConsistencyReport.builder()
                .findings(findings)
                .groupsChecked(groupsChecked)
                .consistentGroups(consistentGroups)
                .build();
    }

    // ── Check B: filename key vs {key:} ──────────────────────────────────────

    private java.util.Optional<Finding> findFilenameKeyMismatch(CatalogEntry variant) {
        SongId id = variant.getSongId();
        String suffix = id.getKeyAlternative();
        if (suffix == null || suffix.isBlank()) {
            return java.util.Optional.empty(); // base version — no filename key
        }
        if (!MusicalKey.isParseable(suffix) || !MusicalKey.isParseable(variant.getKey())) {
            return java.util.Optional.empty(); // can't compare — leave it alone
        }
        MusicalKey filenameKey = MusicalKey.parse(suffix);
        MusicalKey catalogKey  = MusicalKey.parse(variant.getKey());
        if (filenameKey.equals(catalogKey)) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(Finding.builder()
                .type(FindingType.FILENAME_KEY)
                .groupKey(id.toString())
                .detail(String.format("filename key suffix '-%s'  vs  {key:} '%s'",
                        suffix, variant.getKey().trim()))
                .build());
    }

    // ── Check A: cross-variant drift ─────────────────────────────────────────

    private List<Finding> findDrift(String groupKey, List<CatalogEntry> variants,
                                    CatalogEntry source) {
        List<Finding> findings = new ArrayList<>();
        for (CatalogEntry variant : variants) {
            if (variant == source) {
                continue;
            }
            List<String> diffs = comparator.diff(source, variant, comparator.crossVariantFields());
            if (!diffs.isEmpty()) {
                Finding.FindingBuilder fb = Finding.builder()
                        .type(FindingType.DRIFT)
                        .groupKey(groupKey)
                        .detail(String.format("%s  vs  %s",
                                source.getSongId(), variant.getSongId()));
                diffs.forEach(d -> fb.detail("    " + d));
                findings.add(fb.build());
            }
        }
        return findings;
    }

    // ── Check C: missing performance key, inferable from a variant's KEY ──────

    /**
     * Flags groups where NO variant has a PERFORMANCE KEY set, but a non-base
     * variant's own written {@code KEY} is a plausible stand-in (the "variant
     * exists because it's played in a different key" convention). Confident
     * when exactly one such candidate exists; ambiguous (report-only, unless
     * pinned via {@code --source}) when 2+ candidates disagree.
     */
    private Optional<Finding> findMissingPerformanceKey(String groupKey, List<CatalogEntry> variants,
                                                          String sourceSongId) {
        if (!allPerformanceKeysBlank(variants)) {
            return Optional.empty();
        }
        List<CatalogEntry> candidates = keyedNonBaseCandidates(variants);
        if (candidates.isEmpty()) {
            return Optional.empty(); // nothing to infer from
        }
        if (candidates.size() == 1) {
            CatalogEntry variant = candidates.get(0);
            String inferred = CatalogEntryComparator.normalise(variant.getKey());
            return Optional.of(Finding.builder()
                    .type(FindingType.MISSING_PERFORMANCE_KEY)
                    .groupKey(groupKey)
                    .detail(String.format(
                            "no PERFORMANCE KEY set anywhere in the group; variant %s is written in '%s' "
                                    + "-> would set performance key to '%s'",
                            variant.getSongId(), inferred, inferred))
                    .build());
        }

        Optional<CatalogEntry> pinned = candidates.stream()
                .filter(v -> sourceSongId != null && v.getSongId().toString().equals(sourceSongId.trim()))
                .findFirst();
        if (pinned.isPresent()) {
            String inferred = CatalogEntryComparator.normalise(pinned.get().getKey());
            return Optional.of(Finding.builder()
                    .type(FindingType.MISSING_PERFORMANCE_KEY)
                    .groupKey(groupKey)
                    .detail(String.format(
                            "no PERFORMANCE KEY set anywhere in the group; %d variants disagree on written key "
                                    + "-- --source pins %s ('%s') as the performance key",
                            candidates.size(), pinned.get().getSongId(), inferred))
                    .build());
        }

        String options = candidates.stream()
                .map(v -> v.getSongId() + "='" + v.getKey() + "'")
                .collect(Collectors.joining(", "));
        return Optional.of(Finding.builder()
                .type(FindingType.MISSING_PERFORMANCE_KEY)
                .groupKey(groupKey)
                .detail("no PERFORMANCE KEY set anywhere in the group, and " + candidates.size()
                        + " variants disagree on written key (" + options + ") -- ambiguous, not auto-fixed; "
                        + "re-run with --fix --source <one of these song IDs> to pick one")
                .build());
    }

    /** Same resolution logic as {@link #findMissingPerformanceKey}, but returns just the value to apply with --fix. */
    private Optional<String> resolveInferredPerformanceKey(List<CatalogEntry> variants, String sourceSongId) {
        if (!allPerformanceKeysBlank(variants)) {
            return Optional.empty();
        }
        List<CatalogEntry> candidates = keyedNonBaseCandidates(variants);
        if (candidates.size() == 1) {
            return Optional.of(CatalogEntryComparator.normalise(candidates.get(0).getKey()));
        }
        if (candidates.size() > 1 && sourceSongId != null && !sourceSongId.isBlank()) {
            return candidates.stream()
                    .filter(v -> v.getSongId().toString().equals(sourceSongId.trim()))
                    .map(v -> CatalogEntryComparator.normalise(v.getKey()))
                    .findFirst();
        }
        return Optional.empty();
    }

    private void applyInferredPerformanceKey(String inferredKey, List<CatalogEntry> variants,
                                              Map<String, CatalogEntry> catalog) {
        for (CatalogEntry variant : variants) {
            String id = variant.getSongId().toString();
            // Read the CURRENT catalog-map entry, not the pre-fix snapshot in `variants` --
            // a drift fix earlier in this same group's processing may have already
            // updated other fields, and we must not clobber that.
            CatalogEntry current = catalog.get(id);
            catalog.put(id, current.toBuilder().performanceKey(inferredKey).build());
        }
    }

    private boolean allPerformanceKeysBlank(List<CatalogEntry> variants) {
        return variants.stream().allMatch(v -> CatalogEntryComparator.normalise(v.getPerformanceKey()).isBlank());
    }

    /** Non-base variants whose own {@code KEY} is set — the pool of inference candidates. */
    private List<CatalogEntry> keyedNonBaseCandidates(List<CatalogEntry> variants) {
        return variants.stream()
                .filter(v -> !v.getSongId().isBaseVersion())
                .filter(v -> !CatalogEntryComparator.normalise(v.getKey()).isBlank())
                .toList();
    }

    // ── --fix: propagate source metadata into the catalog map ────────────────

    private void propagate(CatalogEntry source, List<CatalogEntry> variants,
                           Map<String, CatalogEntry> catalog) {
        for (CatalogEntry variant : variants) {
            if (variant == source) {
                continue;
            }
            // Copy every shared field from source, keeping the variant's own
            // identity and KEY (the legitimate per-variant lever).
            CatalogEntry fixed = variant.toBuilder()
                    .title(source.getTitle())
                    .artist(source.getArtist())
                    .duration(source.getDuration())
                    .tempo(source.getTempo())
                    .timeSignature(source.getTimeSignature())
                    .countin(source.getCountin())
                    .nord(source.getNord())
                    .roland(source.getRoland())
                    .ve(source.getVe())
                    .backingType(source.getBackingType())
                    .songLabel(source.getSongLabel())
                    .performanceKey(source.getPerformanceKey())
                    .build();
            catalog.put(variant.getSongId().toString(), fixed);
        }
    }

    /**
     * Picks the source-of-truth entry for a group: the named {@code sourceSongId}
     * if supplied and present, else the base (standard-key) variant, else the
     * first variant (so a dry-run of an orphan group can still report drift
     * without crashing). {@code --fix} guards separately against fixing an
     * orphan group.
     */
    private CatalogEntry chooseSource(List<CatalogEntry> variants, String sourceSongId) {
        if (sourceSongId != null && !sourceSongId.isBlank()) {
            return variants.stream()
                    .filter(v -> v.getSongId().toString().equals(sourceSongId.trim()))
                    .findFirst()
                    .orElse(variants.get(0));
        }
        return variants.stream()
                .filter(v -> v.getSongId().isBaseVersion())
                .findFirst()
                .orElse(variants.get(0));
    }

    private boolean matchesNamedSource(CatalogEntry source, String sourceSongId) {
        return sourceSongId != null
                && source.getSongId().toString().equals(sourceSongId.trim());
    }
}
