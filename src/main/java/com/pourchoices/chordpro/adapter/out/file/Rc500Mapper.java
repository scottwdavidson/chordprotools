package com.pourchoices.chordpro.adapter.out.file;

import com.pourchoices.chordpro.application.domain.model.Rc500MemoryBank;
import com.pourchoices.chordpro.application.domain.model.Rc500Slot;
import com.pourchoices.chordpro.application.domain.model.Rc500Track;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Hand-written mapper between the RC-500 XML DTO layer ({@link Rc500SlotDto})
 * and the domain model ({@link Rc500Slot}, {@link Rc500MemoryBank}).
 *
 * <p>The most important conversion this class performs is the RC-500 name
 * encoding: the device stores each slot name as 12 raw ASCII integer codes in
 * XML elements {@code <C01>}…{@code <C12>}.  Names shorter than 12 characters
 * are padded with spaces (ASCII 32); characters outside the printable ASCII
 * range (32–126) are silently replaced with {@code '?'} (63) on encode.
 *
 * <p>Following the codebase convention, MapStruct is intentionally <em>not</em>
 * used here — plain Java keeps the build reproducible across Java 21 patch
 * versions without a fragile annotation-processor ordering shim.
 */
@Component
public class Rc500Mapper {

    // -----------------------------------------------------------------------
    // DTO  →  Domain
    // -----------------------------------------------------------------------

    /**
     * Converts a list of slot DTOs (the full contents of one RC0 file) into a
     * {@link Rc500MemoryBank} domain object.
     */
    public Rc500MemoryBank toMemoryBank(List<Rc500SlotDto> slotDtos) {
        List<Rc500Slot> slots = slotDtos.stream()
                .map(this::toSlot)
                .toList();
        return Rc500MemoryBank.builder()
                .slots(slots)
                .build();
    }

    /**
     * Converts a single {@link Rc500SlotDto} into an {@link Rc500Slot} domain object.
     */
    public Rc500Slot toSlot(Rc500SlotDto dto) {
        return Rc500Slot.builder()
                .slotIndex(dto.getSlotIndex())
                .name(decodeName(dto.getNameCodes()))
                .backingTrack(toTrack(dto.getTrack1()))
                .clickTrack(toTrack(dto.getTrack2()))
                .build();
    }

    // -----------------------------------------------------------------------
    // Domain  →  DTO  (name-overlay only)
    // -----------------------------------------------------------------------

    /**
     * Copies the name from the given domain {@link Rc500Slot} into an existing
     * {@link Rc500SlotDto}, leaving all other DTO fields unchanged.
     *
     * <p>This is the primary update operation used during write-back: the caller
     * first reads the original DTOs from disk, then applies only the changed
     * names from the domain model before serializing.
     *
     * @param slot   domain object whose {@link Rc500Slot#getName()} should be persisted
     * @param target mutable DTO to update in-place
     */
    public void applyNameToDto(Rc500Slot slot, Rc500SlotDto target) {
        target.setNameCodes(encodeName(slot.getName()));
    }

    // -----------------------------------------------------------------------
    // Name encode / decode
    // -----------------------------------------------------------------------

    /**
     * Decodes a 12-element array of ASCII integer codes into a trimmed name string.
     * Trailing spaces are stripped; any value outside the printable ASCII range is
     * replaced with {@code '?'}.
     *
     * @param codes exactly 12 ASCII values from {@code <C01>…<C12>}
     * @return trimmed display name (may be empty but never {@code null})
     */
    public static String decodeName(int[] codes) {
        if (codes == null || codes.length == 0) return "";
        StringBuilder sb = new StringBuilder(12);
        for (int code : codes) {
            char ch = (code >= 32 && code <= 126) ? (char) code : '?';
            sb.append(ch);
        }
        // Strip trailing spaces to recover the human-readable name
        int end = sb.length();
        while (end > 0 && sb.charAt(end - 1) == ' ') end--;
        return sb.substring(0, end);
    }

    /**
     * Encodes a name string into a 12-element ASCII integer code array.
     *
     * <p>Rules:
     * <ul>
     *   <li>Names longer than 12 characters are truncated.</li>
     *   <li>Names shorter than 12 characters are space-padded (ASCII 32).</li>
     *   <li>Characters outside the printable ASCII range (32–126) become {@code '?'} (63).</li>
     * </ul>
     *
     * @param name raw display name (may be {@code null}, treated as empty)
     * @return 12-element array of ASCII integer codes
     */
    public static int[] encodeName(String name) {
        int[] codes = new int[12];
        Arrays.fill(codes, 32); // space-pad by default
        if (name == null) return codes;

        String safe = name.length() > 12 ? name.substring(0, 12) : name;
        for (int i = 0; i < safe.length(); i++) {
            int code = safe.charAt(i);
            codes[i] = (code >= 32 && code <= 126) ? code : 63; // '?'
        }
        return codes;
    }

    // -----------------------------------------------------------------------
    // Track helpers
    // -----------------------------------------------------------------------

    private Rc500Track toTrack(Rc500TrackDto dto) {
        return Rc500Track.builder()
                .playLevel(dto.getPlyLvl())
                .wavStat(dto.getWavStat())
                .wavLen(dto.getWavLen())
                .build();
    }
}
