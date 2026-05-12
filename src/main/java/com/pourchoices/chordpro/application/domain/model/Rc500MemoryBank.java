package com.pourchoices.chordpro.application.domain.model;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Optional;

/**
 * Domain model for one RC-500 memory bank — the complete contents of a single
 * {@code MEMORY1.RC0} or {@code MEMORY2.RC0} file.
 *
 * <p>The RC-500 stores up to 99 memory slots per bank file (slot indices 0–98).
 * Each slot contains a name and two audio tracks.
 *
 * <p>This object is the primary unit exchanged between the application layer and
 * the RC-500 adapter; it does <em>not</em> contain any XML-specific details —
 * those are handled exclusively by the adapter layer.
 *
 * @see Rc500Slot
 */
@Value
@Builder
public class Rc500MemoryBank {

    /**
     * Maximum number of memory slots the RC-500 supports per bank file.
     */
    public static final int MAX_SLOTS = 99;

    /**
     * Maximum character length of a slot name on the RC-500 display.
     */
    public static final int NAME_MAX_LENGTH = 12;

    /**
     * All slots loaded from the bank file, ordered by {@link Rc500Slot#getSlotIndex()}.
     * Never {@code null}; may be empty.
     */
    List<Rc500Slot> slots;

    /**
     * Returns the slot at the given zero-based index, or {@link Optional#empty()}
     * when no slot with that index exists in the bank.
     *
     * @param slotIndex zero-based slot index (0–98)
     */
    public Optional<Rc500Slot> findByIndex(int slotIndex) {
        return slots.stream()
                .filter(s -> s.getSlotIndex() == slotIndex)
                .findFirst();
    }

    /** Total number of slots loaded from the file. */
    public int size() {
        return slots.size();
    }
}
