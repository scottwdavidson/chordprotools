package com.pourchoices.chordpro.application.domain.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Structured identity for a ChordPro song file.
 *
 * <p>Format: {@code clusterPrefix:clusterElement:artist:title[-keyAlternative]}
 * <pre>
 *   ABC:B:BillyJoel:MyLife        ← base (standard key)
 *   ABC:B:BillyJoel:MyLife-c      ← key variant (C)
 *   DEF:E:EltonJohn:RocketMan     ← no variant
 * </pre>
 *
 * <p>The four mandatory segments exactly mirror the file-system directory
 * layout under {@code ./cho/}.  The optional {@code keyAlternative} identifies
 * key-variant files (e.g. {@code c} for the C-key version whose standard-key
 * counterpart lives in the base file).
 *
 * <p>Use {@link ChordProPath} to convert between a {@code SongId} and the
 * full {@code ./cho/…/.cho} file-system path.
 */
@Value
@Builder
public class SongId {

    /**
     * Matches a trailing musical-key suffix: a dash followed by one note
     * letter (a–g, case-insensitive), an optional accidental ({@code #} or
     * {@code b}), and an optional minor indicator ({@code m}).
     * Examples: {@code -c}, {@code -am}, {@code -g#m}, {@code -bb}.
     * Does NOT match non-key tokens like {@code -old} or {@code -MVP}.
     */
    private static final Pattern KEY_ALT_PATTERN = Pattern.compile("-([a-gA-G][#b]?m?)$");

    /**
     * Valid cluster prefixes are 2-3 uppercase letters.
     * 8 three-letter clusters (ABC...VWX) plus the remainder cluster (YZ) = 26 letters exactly.
     */
    private static final Pattern CLUSTER_PREFIX_PATTERN = Pattern.compile("^[A-Z]{2,3}$");

    /** e.g. {@code ABC}, {@code DEF} — top-level library directory. */
    @NonNull String clusterPrefix;

    /** e.g. {@code B} — letter sub-directory within {@code clusterPrefix}. */
    @NonNull String clusterElement;

    /** e.g. {@code BillyJoel} — artist directory name. */
    @NonNull String artist;

    /** e.g. {@code MyLife} — base song stem (no key suffix, no extension). */
    @NonNull String title;

    /**
     * e.g. {@code c}, {@code g#m} — optional key-variant suffix.
     * {@code null} for base (standard-key) files.
     */
    String keyAlternative;

    // -------------------------------------------------------------------------
    // Factory
    // -------------------------------------------------------------------------

    /**
     * Parses a song-ID string into its component parts.
     *
     * @param songIdString e.g. {@code "ABC:B:BillyJoel:MyLife-c"}
     * @return populated {@link SongId}
     * @throws IllegalArgumentException if the string does not have exactly four colon-separated segments
     */
    public static SongId parse(String songIdString) {
        if (songIdString == null || songIdString.isBlank()) {
            throw new IllegalArgumentException("songIdString must not be blank");
        }
        String[] parts = songIdString.split(":", -1);
        if (parts.length != 4) {
            throw new IllegalArgumentException(
                    "Expected 4 colon-separated segments (clusterPrefix:clusterElement:artist:title[-key])"
                    + " but got " + parts.length + " in: \"" + songIdString + "\"");
        }

        if (!CLUSTER_PREFIX_PATTERN.matcher(parts[0]).matches()) {
            throw new IllegalArgumentException(
                    "clusterPrefix must be 2-3 uppercase letters (e.g. ABC or YZ)"
                    + " but got: \"" + parts[0] + "\" in: \"" + songIdString + "\"");
        }

        String lastSegment = parts[3];
        Matcher m = KEY_ALT_PATTERN.matcher(lastSegment);
        final String title;
        final String keyAlternative;
        if (m.find()) {
            keyAlternative = m.group(1);
            title = lastSegment.substring(0, m.start());
        } else {
            title = lastSegment;
            keyAlternative = null;
        }

        return SongId.builder()
                .clusterPrefix(parts[0])
                .clusterElement(parts[1])
                .artist(parts[2])
                .title(title)
                .keyAlternative(keyAlternative)
                .build();
    }

    // -------------------------------------------------------------------------
    // Derived views
    // -------------------------------------------------------------------------

    /**
     * Canonical song-ID string, including any key-alternative suffix.
     * e.g. {@code "ABC:B:BillyJoel:MyLife-c"} or {@code "ABC:B:BillyJoel:MyLife"}
     */
    @Override
    public String toString() {
        String base = clusterPrefix + ":" + clusterElement + ":" + artist + ":" + title;
        return hasKeyAlternative() ? base + "-" + keyAlternative : base;
    }

    /**
     * De-duplication grouping key — the song ID <em>without</em> the
     * key-alternative suffix, so a base file and all its variants resolve to
     * the same group key.
     * e.g. {@code "ABC:B:BillyJoel:MyLife"} for both {@code MyLife} and {@code MyLife-c}.
     */
    public String toGroupKey() {
        return clusterPrefix + ":" + clusterElement + ":" + artist + ":" + title;
    }

    /**
     * Returns {@code true} when this song has no key-alternative suffix, i.e.
     * it is the canonical standard-key (base) version.
     */
    public boolean isBaseVersion() {
        return !hasKeyAlternative();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private boolean hasKeyAlternative() {
        return keyAlternative != null && !keyAlternative.isBlank();
    }
}
