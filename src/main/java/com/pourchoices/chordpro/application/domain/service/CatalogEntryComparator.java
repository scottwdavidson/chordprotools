package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Field-by-field comparison of {@link CatalogEntry} objects.
 *
 * <p>Extracted from {@code VerifyCatalogService} so the same normalisation and
 * field set is reused by every consistency check ({@code verify-catalog} and
 * {@code consistent-metadata}) rather than being copy-pasted (DRY).
 *
 * <p>A single comparable field is modelled as a {@link Field}: a label plus an
 * accessor. Callers choose which subset of fields to compare, which is exactly
 * the difference between the two use-cases:
 * <ul>
 *   <li><b>verify-catalog</b> compares a catalog row against the parsed file —
 *       it uses {@link #allFields()} (minus per-gig RC slot).</li>
 *   <li><b>consistent-metadata</b> compares variants of the same song — it uses
 *       {@link #crossVariantFields()}, which additionally excludes the
 *       per-variant lever KEY.</li>
 * </ul>
 */
@Service
public class CatalogEntryComparator {

    /** A single comparable catalog field: a display label and a value accessor. */
    public record Field(String label, Function<CatalogEntry, String> accessor) {
        String valueOf(CatalogEntry e) {
            return normalise(accessor.apply(e));
        }
    }

    private static String backingStr(CatalogEntry e) {
        return e.getBackingType() != null ? e.getBackingType().name() : "";
    }

    /**
     * All meaningful catalog fields except the per-gig RC slot (which lives in
     * {@code gigs.csv}, not the catalog).
     */
    public List<Field> allFields() {
        List<Field> fields = new ArrayList<>();
        fields.add(new Field("TITLE",       CatalogEntry::getTitle));
        fields.add(new Field("ARTIST",      CatalogEntry::getArtist));
        fields.add(new Field("KEY",         CatalogEntry::getKey));
        fields.add(new Field("DURATION",    CatalogEntry::getDuration));
        fields.add(new Field("TEMPO",       CatalogEntry::getTempo));
        fields.add(new Field("TIME SIG",    CatalogEntry::getTimeSignature));
        fields.add(new Field("COUNTIN",     CatalogEntry::getCountin));
        fields.add(new Field("NORD",        CatalogEntry::getNord));
        fields.add(new Field("ROLAND",      CatalogEntry::getRoland));
        fields.add(new Field("VE",          CatalogEntry::getVe));
        fields.add(new Field("BACKING",     CatalogEntryComparator::backingStr));
        fields.add(new Field("SONG LABEL",  CatalogEntry::getSongLabel));
        fields.add(new Field("PERF KEY",    CatalogEntry::getPerformanceKey));
        return fields;
    }

    /**
     * Fields that must be identical across key-variants of the same song.
     *
     * <p>Excludes KEY — the legitimate per-variant lever (the guitarist may
     * play in a different written key). Everything
     * else, <b>including PERFORMANCE KEY</b> (the sounding key everyone actually
     * plays in), must match.
     */
    public List<Field> crossVariantFields() {
        return allFields().stream()
                .filter(f -> !f.label().equals("KEY"))
                .toList();
    }

    /**
     * Compares two entries over the given fields.
     *
     * @return one human-readable diff string per differing field; empty if equal
     */
    public List<String> diff(CatalogEntry a, CatalogEntry b, List<Field> fields) {
        List<String> diffs = new ArrayList<>();
        for (Field field : fields) {
            String va = field.valueOf(a);
            String vb = field.valueOf(b);
            if (!va.equals(vb)) {
                diffs.add(String.format("%-12s '%s'  vs  '%s'", field.label(), va, vb));
            }
        }
        return diffs;
    }

    /** Trims and null-coalesces a field value for comparison. */
    public static String normalise(String value) {
        return value == null ? "" : value.trim();
    }
}
