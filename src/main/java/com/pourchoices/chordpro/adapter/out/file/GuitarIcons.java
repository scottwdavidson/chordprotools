package com.pourchoices.chordpro.adapter.out.file;

import com.pourchoices.chordpro.application.domain.model.GuitarType;

/**
 * Black-and-white-friendly guitar silhouettes for the floor setlist. Emoji would render as the
 * same guitar on a grayscale printout, so each type gets its own shape instead:
 * <ul>
 *   <li><b>acoustic</b> — big round waisted body with a sound hole, no horns</li>
 *   <li><b>electric</b> — angular horned body with two pickups</li>
 *   <li><b>bass</b> — horned body, much longer neck, four tuners</li>
 *   <li><b>unknown</b> — a "prohibited" sign (circle with a diagonal slash): the guitar for this
 *       song is deliberately undecided, so don't count on it when avoiding a change</li>
 * </ul>
 *
 * <p>The shapes are declared once as SVG {@code <symbol>}s ({@link #definitions()}) and each
 * row just references one with {@code <use>} ({@link #icon(GuitarType)}). A song that hasn't
 * been characterized yet ({@code null}) gets no icon at all.
 */
final class GuitarIcons {

    private static final String VIEW_BOX = "0 0 28 44";

    private GuitarIcons() {
    }

    /** Hidden once-per-document block holding every symbol. */
    static String definitions() {
        return """
                <svg width="0" height="0" style="position:absolute" aria-hidden="true">
                <symbol id="guitar-acoustic" viewBox="%1$s">
                  <rect x="12" y="0" width="4" height="7" rx="1"/>
                  <rect x="13" y="6" width="2" height="22"/>
                  <ellipse cx="14" cy="26" rx="7" ry="5.5"/>
                  <ellipse cx="14" cy="35" rx="13" ry="9"/>
                  <circle cx="14" cy="30.5" r="3.4" fill="#fff"/>
                  <rect x="9" y="38.5" width="10" height="1.6" fill="#fff"/>
                </symbol>
                <symbol id="guitar-electric" viewBox="%1$s">
                  <rect x="12" y="0" width="4" height="7" rx="1"/>
                  <rect x="13" y="6" width="2" height="24"/>
                  <path d="M6 22 L12 22 L12 28 L16 28 L16 22 L22 22 L21 30 L27 33 L26 41 L20 44 L8 44 L2 41 L1 33 L7 30 Z"/>
                  <rect x="8" y="33" width="12" height="2" fill="#fff"/>
                  <rect x="8" y="37.5" width="12" height="2" fill="#fff"/>
                </symbol>
                <symbol id="guitar-bass" viewBox="%1$s">
                  <rect x="11" y="0" width="6" height="10" rx="1"/>
                  <rect x="7" y="1" width="4" height="1.8"/><rect x="7" y="5.5" width="4" height="1.8"/>
                  <rect x="17" y="1" width="4" height="1.8"/><rect x="17" y="5.5" width="4" height="1.8"/>
                  <rect x="12.5" y="9" width="3" height="28"/>
                  <path d="M8 31 L12 31 L12 35 L16 35 L16 31 L20 31 L20 36 L25 38 L24 43 L19 44 L9 44 L4 43 L3 38 L8 36 Z"/>
                  <rect x="10" y="39" width="8" height="1.6" fill="#fff"/>
                </symbol>
                <symbol id="guitar-unknown" viewBox="%1$s">
                  <circle cx="14" cy="22" r="12" fill="none" stroke="#000" stroke-width="3.8"/>
                  <line x1="5.5" y1="13.5" x2="22.5" y2="30.5" stroke="#000" stroke-width="3.8"/>
                </symbol>
                </svg>
                """.formatted(VIEW_BOX);
    }

    /** The {@code <svg>} reference for this guitar, or an empty string when there's nothing to show. */
    static String icon(GuitarType guitar) {
        if (guitar == null) return "";
        String name = switch (guitar) {
            case ACOUSTIC -> "acoustic";
            case ELECTRIC -> "electric";
            case BASS -> "bass";
            case UNKNOWN -> "unknown";
        };
        return "<svg class=\"guitar-icon\" viewBox=\"" + VIEW_BOX + "\" role=\"img\" aria-label=\"" + name
                + " guitar\"><use href=\"#guitar-" + name + "\"/></svg>";
    }
}
