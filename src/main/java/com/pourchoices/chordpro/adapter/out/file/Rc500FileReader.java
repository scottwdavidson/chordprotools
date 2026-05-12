package com.pourchoices.chordpro.adapter.out.file;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses an RC-500 {@code .RC0} memory bank XML file into a list of
 * {@link Rc500SlotDto} objects, one per {@code <mem>} element.
 *
 * <p>Uses the JDK's built-in DOM parser — no additional XML library dependency
 * is required.  All 99 slots in the file are parsed, even those that contain no
 * audio (they still carry the factory-default XML structure).
 */
@Component
@Slf4j
public class Rc500FileReader {

    /**
     * Reads the given RC0 file and returns all memory slot DTOs found inside it.
     *
     * @param rc0FilePath path to the {@code .RC0} file
     * @return ordered list of DTOs (ordered by slot index ascending)
     * @throws Rc500ParseException if the file cannot be parsed
     */
    public List<Rc500SlotDto> readSlots(Path rc0FilePath) {
        log.debug("Parsing RC0 file: {}", rc0FilePath);
        Document doc = parseXml(rc0FilePath);
        NodeList memNodes = doc.getElementsByTagName("mem");
        List<Rc500SlotDto> slots = new ArrayList<>(memNodes.getLength());

        for (int i = 0; i < memNodes.getLength(); i++) {
            Element memEl = (Element) memNodes.item(i);
            slots.add(parseSlot(memEl));
        }

        log.debug("Parsed {} memory slots from {}", slots.size(), rc0FilePath);
        return slots;
    }

    // -----------------------------------------------------------------------
    // Per-slot parsing
    // -----------------------------------------------------------------------

    private Rc500SlotDto parseSlot(Element memEl) {
        int slotIndex = Integer.parseInt(memEl.getAttribute("id"));

        int[] nameCodes = parseName(child(memEl, "NAME"));

        Element track1El = child(memEl, "TRACK1");
        Element track2El = child(memEl, "TRACK2");
        Element masterEl = child(memEl, "MASTER");
        Element loopFxEl = child(memEl, "LOOPFX");
        Element rhythmEl = child(memEl, "RHYTHM");
        Element ctlEl    = child(memEl, "CTL");

        Rc500SlotDto dto = Rc500SlotDto.builder()
                .slotIndex(slotIndex)
                .nameCodes(nameCodes)
                .track1(parseTrack(track1El))
                .track2(parseTrack(track2El))
                // MASTER
                .masterTempo(intVal(masterEl, "Tempo"))
                .masterDubMode(intVal(masterEl, "DubMode"))
                .masterRecAction(intVal(masterEl, "RecAction"))
                .masterRecQuantize(intVal(masterEl, "RecQuantize"))
                .masterAutoRec(intVal(masterEl, "AutoRec"))
                .masterAutoRecSens(intVal(masterEl, "AutoRecSens"))
                .masterAutoRecSrc(intVal(masterEl, "AutoRecSrc"))
                .masterPlayMode(intVal(masterEl, "PlayMode"))
                .masterSinglPlayeChange(intVal(masterEl, "SinglPlayeChange"))
                .masterFadeTime(intVal(masterEl, "FadeTime"))
                .masterAllStart(intVal(masterEl, "AllStart"))
                .masterTrackChain(intVal(masterEl, "TrackChain"))
                .masterCurrentTrack(intVal(masterEl, "CurrentTrack"))
                .masterAllTrackSel(intVal(masterEl, "AllTrackSel"))
                .masterLevel(intVal(masterEl, "Level"))
                .masterLpMod(intVal(masterEl, "LpMod"))
                .masterLpLen(intVal(masterEl, "LpLen"))
                .masterTrkMod(intVal(masterEl, "TrkMod"))
                .masterSync(intVal(masterEl, "Sync"))
                // LOOPFX
                .loopFxSw(intVal(loopFxEl, "Sw"))
                .loopFxType(intVal(loopFxEl, "FxType"))
                .loopFxRepeatLength(intVal(loopFxEl, "RepeatLength"))
                .loopFxShiftShift(intVal(loopFxEl, "ShiftShift"))
                .loopFxScatterLength(intVal(loopFxEl, "ScatterLength"))
                .loopFxVinylFlickFlick(intVal(loopFxEl, "VinylFlickFlick"))
                // RHYTHM
                .rhythmLevel(intVal(rhythmEl, "Level"))
                .rhythmReverb(intVal(rhythmEl, "Reverb"))
                .rhythmPattern(intVal(rhythmEl, "Pattern"))
                .rhythmVariation(intVal(rhythmEl, "Variation"))
                .rhythmVariationChange(intVal(rhythmEl, "VariationChange"))
                .rhythmKit(intVal(rhythmEl, "Kit"))
                .rhythmBeat(intVal(rhythmEl, "Beat"))
                .rhythmFill(intVal(rhythmEl, "Fill"))
                .rhythmPart1(intVal(rhythmEl, "Part1"))
                .rhythmPart2(intVal(rhythmEl, "Part2"))
                .rhythmPart3(intVal(rhythmEl, "Part3"))
                .rhythmPart4(intVal(rhythmEl, "Part4"))
                .rhythmRecCount(intVal(rhythmEl, "RecCount"))
                .rhythmPlayCount(intVal(rhythmEl, "PlayCount"))
                .rhythmStart(intVal(rhythmEl, "Start"))
                .rhythmStop(intVal(rhythmEl, "Stop"))
                .rhythmToneLow(intVal(rhythmEl, "ToneLow"))
                .rhythmToneHigh(intVal(rhythmEl, "ToneHigh"))
                .rhythmState(intVal(rhythmEl, "State"))
                // CTL
                .ctlPedal1(intVal(ctlEl, "Pedal1"))
                .ctlPedal2(intVal(ctlEl, "Pedal2"))
                .ctlPedal3(intVal(ctlEl, "Pedal3"))
                .ctlCtl1(intVal(ctlEl, "Ctl1"))
                .ctlCtl2(intVal(ctlEl, "Ctl2"))
                .ctlExp(intVal(ctlEl, "Exp"))
                // ASSIGNs
                .assigns(parseAssigns(memEl))
                .build();

        return dto;
    }

    private int[] parseName(Element nameEl) {
        int[] codes = new int[12];
        for (int i = 1; i <= 12; i++) {
            String tag = String.format("C%02d", i);
            codes[i - 1] = intVal(nameEl, tag);
        }
        return codes;
    }

    private Rc500TrackDto parseTrack(Element trackEl) {
        return Rc500TrackDto.builder()
                .rev(intVal(trackEl, "Rev"))
                .plyLvl(intVal(trackEl, "PlyLvl"))
                .pan(intVal(trackEl, "Pan"))
                .one(intVal(trackEl, "One"))
                .loopFx(intVal(trackEl, "LoopFx"))
                .strtMod(intVal(trackEl, "StrtMod"))
                .stpMod(intVal(trackEl, "StpMod"))
                .measure(intVal(trackEl, "Measure"))
                .loopSync(intVal(trackEl, "LoopSync"))
                .tempoSync(intVal(trackEl, "TempoSync"))
                .input(intVal(trackEl, "Input"))
                .output(intVal(trackEl, "Output"))
                .measMod(intVal(trackEl, "MeasMod"))
                .measLen(intVal(trackEl, "MeasLen"))
                .measBtLp(intVal(trackEl, "MeasBtLp"))
                .recTmp(intVal(trackEl, "RecTmp"))
                .wavStat(intVal(trackEl, "WavStat"))
                .wavLen(intVal(trackEl, "WavLen"))
                .build();
    }

    private List<Rc500AssignDto> parseAssigns(Element memEl) {
        List<Rc500AssignDto> assigns = new ArrayList<>(8);
        for (int i = 1; i <= 8; i++) {
            Element assignEl = child(memEl, "ASSIGN" + i);
            assigns.add(Rc500AssignDto.builder()
                    .sw(intVal(assignEl, "Sw"))
                    .source(intVal(assignEl, "Source"))
                    .sourceMode(intVal(assignEl, "SourceMode"))
                    .target(intVal(assignEl, "Target"))
                    .targetMin(intVal(assignEl, "TargetMin"))
                    .targetMax(intVal(assignEl, "TargetMax"))
                    .build());
        }
        return assigns;
    }

    // -----------------------------------------------------------------------
    // XML parsing helpers
    // -----------------------------------------------------------------------

    private Document parseXml(Path path) {
        try {
            byte[] raw = Files.readAllBytes(path);
            byte[] xmlBytes = truncateAtClosingTag(raw, path);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
            return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xmlBytes));
        } catch (Rc500ParseException e) {
            throw e;
        } catch (Exception e) {
            throw new Rc500ParseException("Failed to parse RC0 file: " + path, e);
        }
    }

    /**
     * RC-500 firmware appends a binary footer after the {@code </database>} closing tag.
     * This method finds the last byte of the XML content and returns only those bytes
     * so the DOM parser never sees the trailing binary garbage.
     */
    private byte[] truncateAtClosingTag(byte[] raw, Path path) throws IOException {
        byte[] marker = "</database>".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        // Search backwards from the end for the closing tag
        outer:
        for (int i = raw.length - marker.length; i >= 0; i--) {
            for (int j = 0; j < marker.length; j++) {
                if (raw[i + j] != marker[j]) continue outer;
            }
            // Found it — include the tag itself (plus optional trailing newline)
            int end = i + marker.length;
            if (end < raw.length && raw[end] == '\n') end++;
            log.debug("Truncating RC0 file at byte {} (original size {})", end, raw.length);
            byte[] truncated = new byte[end];
            System.arraycopy(raw, 0, truncated, 0, end);
            return truncated;
        }
        // No closing tag found — attempt parse as-is and let the parser report the error
        log.warn("Could not find </database> in {}; attempting parse of full file", path);
        return raw;
    }

    /**
     * Returns the first direct child {@link Element} with the given tag name.
     *
     * @throws Rc500ParseException if no such child exists
     */
    private Element child(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) {
            throw new Rc500ParseException(
                    "Expected element <" + tagName + "> inside <" + parent.getTagName() + ">");
        }
        return (Element) nodes.item(0);
    }

    /**
     * Reads the text content of a named child element as an integer.
     *
     * @throws Rc500ParseException if the element is missing or its value is not a valid integer
     */
    private int intVal(Element parent, String tagName) {
        Element el = child(parent, tagName);
        String text = el.getTextContent().trim();
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new Rc500ParseException(
                    "Expected integer in <" + tagName + "> but got: \"" + text + "\"", e);
        }
    }
}
