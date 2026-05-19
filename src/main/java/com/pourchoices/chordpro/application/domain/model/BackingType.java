package com.pourchoices.chordpro.application.domain.model;

/**
 * The device that provides the backing track for a song.
 *
 * <ul>
 *   <li>{@link #RC} — Roland RC-500 loop station. The song has a purchased
 *       backing track loaded at a specific slot ({@code rcSlot} in {@link CatalogEntry}).</li>
 *   <li>{@link #BB} — Beat Buddy drum machine. Drums only; no slot number.</li>
 * </ul>
 *
 * <p>Absence of a value (null / blank in CSV or {@code .cho} file) means the
 * song has no backing device.
 */
public enum BackingType {

    RC, BB;

    /**
     * Parses a backing type string from a catalog CSV or {@code .cho} file.
     *
     * <p>Returns {@code null} for null, blank, the legacy sentinel {@code "99"},
     * or any numeric string (legacy RC slot numbers stored before this field was
     * split into type + slot).
     */
    public static BackingType fromString(String value) {
        if (value == null || value.isBlank()) return null;
        return switch (value.trim().toUpperCase()) {
            case "RC" -> RC;
            case "BB" -> BB;
            default   -> null;  // legacy numeric slot or unknown — treated as absent
        };
    }
}
