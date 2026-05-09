package com.pourchoices.chordpro.application.domain.model;

/**
 * Converts between a {@link SongId} and the full ChordPro file-system path.
 *
 * <p>Convention:
 * <pre>
 *   file path  =  "./cho/"  +  songId.toString()  +  ".cho"
 *
 *   SongId "ABC/B/BillyJoel/MyLife-c"  →  "./cho/ABC/B/BillyJoel/MyLife-c.cho"
 *   "./cho/ABC/B/BillyJoel/MyLife.cho" →  SongId "ABC/B/BillyJoel/MyLife"
 * </pre>
 *
 * <p>This is a utility class — it must not be instantiated.
 */
public final class ChordProPath {

    private static final String BASE_PATH = "./cho/";
    private static final String EXTENSION  = ".cho";

    private ChordProPath() {}

    /**
     * Reconstructs the full file-system path from a {@link SongId}.
     *
     * @param songId the song identity
     * @return e.g. {@code "./cho/ABC/B/BillyJoel/MyLife-c.cho"}
     */
    public static String toFilePath(SongId songId) {
        return BASE_PATH + songId.toString() + EXTENSION;
    }

    /**
     * Derives a {@link SongId} from a full file-system path by stripping the
     * {@code "./cho/"} prefix and the {@code ".cho"} extension.
     *
     * @param filePath e.g. {@code "./cho/ABC/B/BillyJoel/MyLife-c.cho"}
     * @return the parsed {@link SongId}
     * @throws IllegalArgumentException if the resulting song-ID string is invalid
     */
    public static SongId toSongId(String filePath) {
        String songIdString = filePath
                .replaceFirst("^\\./cho/", "")
                .replaceAll("\\.cho$", "");
        return SongId.parse(songIdString);
    }
}
