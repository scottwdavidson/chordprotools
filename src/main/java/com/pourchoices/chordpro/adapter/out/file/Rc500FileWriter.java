package com.pourchoices.chordpro.adapter.out.file;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Serializes a list of {@link Rc500SlotDto} objects back to the RC-500 XML file
 * format ({@code MEMORY1.RC0} / {@code MEMORY2.RC0}).
 *
 * <p>The output exactly matches the RC-500's own file format:
 * <ul>
 *   <li>UTF-8 encoding, LF line endings.</li>
 *   <li>Tab-indented XML with no XML declaration namespace declarations.</li>
 *   <li>Slot order preserved as supplied (caller is responsible for ordering).</li>
 * </ul>
 *
 * <p>Uses a hand-written {@link StringBuilder} approach rather than a DOM
 * {@code Transformer} to guarantee byte-for-byte reproducibility of the
 * whitespace and indentation that the RC-500 firmware expects.
 */
@Component
@Slf4j
public class Rc500FileWriter {

    private static final String XML_DECLARATION =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>";

    /**
     * Writes the complete RC0 file to {@code destination}.
     *
     * @param destination  path of the file to create or overwrite
     * @param slots        all memory slots to include, in index order
     * @throws Rc500ParseException if the file cannot be written
     */
    public void writeSlots(Path destination, List<Rc500SlotDto> slots) {
        log.debug("Writing {} RC0 memory slots to {}", slots.size(), destination);
        String xml = buildXml(slots);
        try (BufferedWriter writer = Files.newBufferedWriter(destination, StandardCharsets.UTF_8)) {
            writer.write(xml);
        } catch (IOException e) {
            throw new Rc500ParseException("Failed to write RC0 file: " + destination, e);
        }
        log.debug("RC0 file written successfully: {}", destination);
    }

    // -----------------------------------------------------------------------
    // XML construction
    // -----------------------------------------------------------------------

    private String buildXml(List<Rc500SlotDto> slots) {
        StringBuilder sb = new StringBuilder(slots.size() * 3_500);
        sb.append(XML_DECLARATION).append('\n');
        sb.append("<database name=\"RC-500\" revision=\"0\">\n");
        for (Rc500SlotDto slot : slots) {
            appendSlot(sb, slot);
        }
        sb.append("</database>\n");
        return sb.toString();
    }

    private void appendSlot(StringBuilder sb, Rc500SlotDto s) {
        sb.append("<mem id=\"").append(s.getSlotIndex()).append("\">\n");
        appendName(sb, s.getNameCodes());
        appendTrack(sb, "TRACK1", s.getTrack1());
        appendTrack(sb, "TRACK2", s.getTrack2());
        appendMaster(sb, s);
        appendLoopFx(sb, s);
        appendRhythm(sb, s);
        appendCtl(sb, s);
        appendAssigns(sb, s.getAssigns());
        sb.append("</mem>\n");
    }

    private void appendName(StringBuilder sb, int[] codes) {
        sb.append("<NAME>\n");
        for (int i = 0; i < 12; i++) {
            sb.append('\t').append('<').append(String.format("C%02d", i + 1)).append('>')
              .append(codes[i])
              .append("</").append(String.format("C%02d", i + 1)).append(">\n");
        }
        sb.append("</NAME>\n");
    }

    private void appendTrack(StringBuilder sb, String tag, Rc500TrackDto t) {
        sb.append("<").append(tag).append(">\n");
        tab(sb, "Rev",       t.getRev());
        tab(sb, "PlyLvl",   t.getPlyLvl());
        tab(sb, "Pan",       t.getPan());
        tab(sb, "One",       t.getOne());
        tab(sb, "LoopFx",   t.getLoopFx());
        tab(sb, "StrtMod",  t.getStrtMod());
        tab(sb, "StpMod",   t.getStpMod());
        tab(sb, "Measure",  t.getMeasure());
        tab(sb, "LoopSync", t.getLoopSync());
        tab(sb, "TempoSync",t.getTempoSync());
        tab(sb, "Input",    t.getInput());
        tab(sb, "Output",   t.getOutput());
        tab(sb, "MeasMod",  t.getMeasMod());
        tab(sb, "MeasLen",  t.getMeasLen());
        tab(sb, "MeasBtLp", t.getMeasBtLp());
        tab(sb, "RecTmp",   t.getRecTmp());
        tab(sb, "WavStat",  t.getWavStat());
        tab(sb, "WavLen",   t.getWavLen());
        sb.append("</").append(tag).append(">\n");
    }

    private void appendMaster(StringBuilder sb, Rc500SlotDto s) {
        sb.append("<MASTER>\n");
        tab(sb, "Tempo",            s.getMasterTempo());
        tab(sb, "DubMode",          s.getMasterDubMode());
        tab(sb, "RecAction",        s.getMasterRecAction());
        tab(sb, "RecQuantize",      s.getMasterRecQuantize());
        tab(sb, "AutoRec",          s.getMasterAutoRec());
        tab(sb, "AutoRecSens",      s.getMasterAutoRecSens());
        tab(sb, "AutoRecSrc",       s.getMasterAutoRecSrc());
        tab(sb, "PlayMode",         s.getMasterPlayMode());
        tab(sb, "SinglPlayeChange", s.getMasterSinglPlayeChange());
        tab(sb, "FadeTime",         s.getMasterFadeTime());
        tab(sb, "AllStart",         s.getMasterAllStart());
        tab(sb, "TrackChain",       s.getMasterTrackChain());
        tab(sb, "CurrentTrack",     s.getMasterCurrentTrack());
        tab(sb, "AllTrackSel",      s.getMasterAllTrackSel());
        tab(sb, "Level",            s.getMasterLevel());
        tab(sb, "LpMod",            s.getMasterLpMod());
        tab(sb, "LpLen",            s.getMasterLpLen());
        tab(sb, "TrkMod",           s.getMasterTrkMod());
        tab(sb, "Sync",             s.getMasterSync());
        sb.append("</MASTER>\n");
    }

    private void appendLoopFx(StringBuilder sb, Rc500SlotDto s) {
        sb.append("<LOOPFX>\n");
        tab(sb, "Sw",              s.getLoopFxSw());
        tab(sb, "FxType",          s.getLoopFxType());
        tab(sb, "RepeatLength",    s.getLoopFxRepeatLength());
        tab(sb, "ShiftShift",      s.getLoopFxShiftShift());
        tab(sb, "ScatterLength",   s.getLoopFxScatterLength());
        tab(sb, "VinylFlickFlick", s.getLoopFxVinylFlickFlick());
        sb.append("</LOOPFX>\n");
    }

    private void appendRhythm(StringBuilder sb, Rc500SlotDto s) {
        sb.append("<RHYTHM>\n");
        tab(sb, "Level",           s.getRhythmLevel());
        tab(sb, "Reverb",          s.getRhythmReverb());
        tab(sb, "Pattern",         s.getRhythmPattern());
        tab(sb, "Variation",       s.getRhythmVariation());
        tab(sb, "VariationChange", s.getRhythmVariationChange());
        tab(sb, "Kit",             s.getRhythmKit());
        tab(sb, "Beat",            s.getRhythmBeat());
        tab(sb, "Fill",            s.getRhythmFill());
        tab(sb, "Part1",           s.getRhythmPart1());
        tab(sb, "Part2",           s.getRhythmPart2());
        tab(sb, "Part3",           s.getRhythmPart3());
        tab(sb, "Part4",           s.getRhythmPart4());
        tab(sb, "RecCount",        s.getRhythmRecCount());
        tab(sb, "PlayCount",       s.getRhythmPlayCount());
        tab(sb, "Start",           s.getRhythmStart());
        tab(sb, "Stop",            s.getRhythmStop());
        tab(sb, "ToneLow",         s.getRhythmToneLow());
        tab(sb, "ToneHigh",        s.getRhythmToneHigh());
        tab(sb, "State",           s.getRhythmState());
        sb.append("</RHYTHM>\n");
    }

    private void appendCtl(StringBuilder sb, Rc500SlotDto s) {
        sb.append("<CTL>\n");
        tab(sb, "Pedal1", s.getCtlPedal1());
        tab(sb, "Pedal2", s.getCtlPedal2());
        tab(sb, "Pedal3", s.getCtlPedal3());
        tab(sb, "Ctl1",   s.getCtlCtl1());
        tab(sb, "Ctl2",   s.getCtlCtl2());
        tab(sb, "Exp",    s.getCtlExp());
        sb.append("</CTL>\n");
    }

    private void appendAssigns(StringBuilder sb, List<Rc500AssignDto> assigns) {
        for (int i = 0; i < assigns.size(); i++) {
            Rc500AssignDto a = assigns.get(i);
            String tag = "ASSIGN" + (i + 1);
            sb.append("<").append(tag).append(">\n");
            tab(sb, "Sw",         a.getSw());
            tab(sb, "Source",     a.getSource());
            tab(sb, "SourceMode", a.getSourceMode());
            tab(sb, "Target",     a.getTarget());
            tab(sb, "TargetMin",  a.getTargetMin());
            tab(sb, "TargetMax",  a.getTargetMax());
            sb.append("</").append(tag).append(">\n");
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Appends a tab-indented {@code <tag>value</tag>} line. */
    private void tab(StringBuilder sb, String tag, int value) {
        sb.append('\t').append('<').append(tag).append('>')
          .append(value)
          .append("</").append(tag).append(">\n");
    }
}
