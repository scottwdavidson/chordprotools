package com.pourchoices.chordpro.adapter.out.file;

import com.pourchoices.chordpro.application.domain.model.Rc500MemoryBank;
import com.pourchoices.chordpro.application.domain.model.Rc500Slot;
import com.pourchoices.chordpro.application.port.out.Rc500Port;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Adapter that fulfils {@link Rc500Port} using file-based RC0 XML I/O.
 *
 * <p><b>Read path:</b> delegates XML parsing to {@link Rc500FileReader}, then
 * maps each raw {@link Rc500SlotDto} to an {@link Rc500Slot} domain object via
 * {@link Rc500Mapper}.
 *
 * <p><b>Write path:</b> uses a <em>read-modify-write</em> strategy to guarantee
 * round-trip fidelity.  The existing file is re-parsed so that every RC-500
 * setting the domain model does not explicitly own (rhythm pattern, loop FX,
 * assignable controllers, etc.) is preserved verbatim.  Only the slot name is
 * overlaid from the {@link Rc500MemoryBank} supplied by the caller.
 */
@Service
@Slf4j
public class Rc500Adapter implements Rc500Port {

    private final Rc500FileReader fileReader;
    private final Rc500FileWriter fileWriter;
    private final Rc500Mapper     mapper;

    public Rc500Adapter(Rc500FileReader fileReader,
                        Rc500FileWriter fileWriter,
                        Rc500Mapper     mapper) {
        this.fileReader = fileReader;
        this.fileWriter = fileWriter;
        this.mapper     = mapper;
    }

    // -----------------------------------------------------------------------
    // Rc500Port implementation
    // -----------------------------------------------------------------------

    @Override
    public Rc500MemoryBank readMemoryBank(Path rc0FilePath) {
        log.info("Reading RC-500 memory bank from: {}", rc0FilePath);
        List<Rc500SlotDto> dtos = fileReader.readSlots(rc0FilePath);
        Rc500MemoryBank bank = mapper.toMemoryBank(dtos);
        log.info("Loaded {} memory slots from {}", bank.size(), rc0FilePath);
        return bank;
    }

    @Override
    public void writeMemoryBank(Path rc0FilePath, Rc500MemoryBank updatedBank) {
        log.info("Writing RC-500 memory bank to: {} ({} updated slots)",
                rc0FilePath, updatedBank.size());

        // 1. Read original DTOs — these carry all the data we are NOT touching.
        List<Rc500SlotDto> originalDtos = fileReader.readSlots(rc0FilePath);

        // 2. Build a lookup by slot index so we can patch just the relevant slots.
        Map<Integer, Rc500Slot> updatedByIndex = updatedBank.getSlots().stream()
                .collect(Collectors.toMap(Rc500Slot::getSlotIndex, s -> s));

        // 3. Overlay only the name from each updated slot onto the original DTO.
        for (Rc500SlotDto dto : originalDtos) {
            Rc500Slot updated = updatedByIndex.get(dto.getSlotIndex());
            if (updated != null) {
                mapper.applyNameToDto(updated, dto);
                log.debug("Slot {:02d}: name set to \"{}\"",
                        dto.getSlotIndex(), updated.getName());
            }
        }

        // 4. Write the patched DTOs back to disk.
        fileWriter.writeSlots(rc0FilePath, originalDtos);
        log.info("RC-500 memory bank written successfully to {}", rc0FilePath);
    }
}
