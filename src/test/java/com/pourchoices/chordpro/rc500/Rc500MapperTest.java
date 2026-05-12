package com.pourchoices.chordpro.rc500;

import com.pourchoices.chordpro.adapter.out.file.Rc500FileReader;
import com.pourchoices.chordpro.adapter.out.file.Rc500Mapper;
import com.pourchoices.chordpro.adapter.out.file.Rc500SlotDto;
import com.pourchoices.chordpro.application.domain.model.Rc500MemoryBank;
import com.pourchoices.chordpro.application.domain.model.Rc500Slot;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Rc500Mapper} name encode/decode and a live parse of the
 * real {@code MEMORY1.RC0} fixture file.
 */
class Rc500MapperTest {

    private final Rc500Mapper mapper = new Rc500Mapper();

    // -----------------------------------------------------------------------
    // Name decode
    // -----------------------------------------------------------------------

    @Test
    void decodeName_returnsExpectedString() {
        // "Memory01  " = 77,101,109,111,114,121,48,49,32,32,32,32
        int[] codes = {77, 101, 109, 111, 114, 121, 48, 49, 32, 32, 32, 32};
        assertThat(Rc500Mapper.decodeName(codes)).isEqualTo("Memory01");
    }

    @Test
    void decodeName_allSpaces_returnsEmpty() {
        int[] spaces = new int[12];
        java.util.Arrays.fill(spaces, 32);
        assertThat(Rc500Mapper.decodeName(spaces)).isEmpty();
    }

    @Test
    void decodeName_nullReturnsEmpty() {
        assertThat(Rc500Mapper.decodeName(null)).isEmpty();
    }

    @Test
    void decodeName_nonPrintableReplacedWithQuestion() {
        int[] codes = new int[12];
        codes[0] = 1; // non-printable
        codes[1] = 65; // 'A'
        java.util.Arrays.fill(codes, 2, 12, 32);
        assertThat(Rc500Mapper.decodeName(codes)).isEqualTo("?A");
    }

    // -----------------------------------------------------------------------
    // Name encode
    // -----------------------------------------------------------------------

    @Test
    void encodeName_shortName_spacePadded() {
        int[] codes = Rc500Mapper.encodeName("Hi");
        assertThat(codes).hasSize(12);
        assertThat(codes[0]).isEqualTo('H');
        assertThat(codes[1]).isEqualTo('i');
        for (int i = 2; i < 12; i++) {
            assertThat(codes[i]).as("position %d should be space", i).isEqualTo(32);
        }
    }

    @Test
    void encodeName_longName_truncatedAt12() {
        int[] codes = Rc500Mapper.encodeName("ABCDEFGHIJKLMNOP"); // 16 chars
        assertThat(codes).hasSize(12);
        assertThat(codes[11]).isEqualTo('L'); // 12th character
    }

    @Test
    void encodeName_nullTreatedAsEmpty() {
        int[] codes = Rc500Mapper.encodeName(null);
        for (int code : codes) assertThat(code).isEqualTo(32);
    }

    @Test
    void encodeName_nonAsciiReplacedWithQuestion() {
        int[] codes = Rc500Mapper.encodeName("A\u00e9B"); // é is non-ASCII
        assertThat(codes[0]).isEqualTo('A');
        assertThat(codes[1]).isEqualTo('?');
        assertThat(codes[2]).isEqualTo('B');
    }

    // -----------------------------------------------------------------------
    // Round-trip: encode then decode
    // -----------------------------------------------------------------------

    @Test
    void roundTrip_encodeDecodeProducesOriginalName() {
        String original = "HeyJude";
        String result = Rc500Mapper.decodeName(Rc500Mapper.encodeName(original));
        assertThat(result).isEqualTo(original);
    }

    @Test
    void roundTrip_exactly12Chars_noTruncation() {
        String original = "123456789012";
        String result = Rc500Mapper.decodeName(Rc500Mapper.encodeName(original));
        assertThat(result).isEqualTo(original);
    }

    // -----------------------------------------------------------------------
    // Live parse of MEMORY1.RC0
    // -----------------------------------------------------------------------

    @Test
    void parsesMemory1Rc0_correctSlotCount() {
        Path rc0 = Paths.get("rc0/MEMORY1.RC0");
        org.junit.jupiter.api.Assumptions.assumeTrue(rc0.toFile().exists(),
                "Skipping live RC0 test — file not present at " + rc0.toAbsolutePath());

        Rc500FileReader reader = new Rc500FileReader();
        List<Rc500SlotDto> dtos = reader.readSlots(rc0);
        Rc500MemoryBank bank = mapper.toMemoryBank(dtos);

        assertThat(bank.size()).isEqualTo(99);
    }

    @Test
    void parsesMemory1Rc0_slotIndicesAreSequential() {
        Path rc0 = Paths.get("rc0/MEMORY1.RC0");
        org.junit.jupiter.api.Assumptions.assumeTrue(rc0.toFile().exists(),
                "Skipping live RC0 test — file not present");

        Rc500FileReader reader = new Rc500FileReader();
        Rc500MemoryBank bank = mapper.toMemoryBank(reader.readSlots(rc0));

        for (int i = 0; i < bank.size(); i++) {
            assertThat(bank.getSlots().get(i).getSlotIndex()).isEqualTo(i);
        }
    }

    @Test
    void parsesMemory1Rc0_slot0NameMatchesKnownDefault() {
        Path rc0 = Paths.get("rc0/MEMORY1.RC0");
        org.junit.jupiter.api.Assumptions.assumeTrue(rc0.toFile().exists(),
                "Skipping live RC0 test — file not present");

        Rc500FileReader reader = new Rc500FileReader();
        Rc500MemoryBank bank = mapper.toMemoryBank(reader.readSlots(rc0));

        // From the file: C07=48 ('0'), C08=49 ('1') → "Memory01"
        Rc500Slot slot0 = bank.findByIndex(0).orElseThrow();
        assertThat(slot0.getName()).isEqualTo("Memory01");
        assertThat(slot0.displayNumber()).isEqualTo(1);
    }

    @Test
    void parsesMemory1Rc0_tracksHaveNoAudioByDefault() {
        Path rc0 = Paths.get("rc0/MEMORY1.RC0");
        org.junit.jupiter.api.Assumptions.assumeTrue(rc0.toFile().exists(),
                "Skipping live RC0 test — file not present");

        Rc500FileReader reader = new Rc500FileReader();
        Rc500MemoryBank bank = mapper.toMemoryBank(reader.readSlots(rc0));

        // The reference file is a factory-default backup — no tracks should have audio.
        long slotsWithAudio = bank.getSlots().stream()
                .filter(Rc500Slot::hasAnyAudio)
                .count();
        assertThat(slotsWithAudio).isEqualTo(0);
    }
}
