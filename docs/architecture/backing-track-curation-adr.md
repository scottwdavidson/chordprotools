# ADR 001: Streamlined Backing and Practice Track Ingestion Pipeline for Live Performance (Boss RC-500)

* **Status:** Accepted
* **Date:** September 9, 2026
* **Deciders:** Scott Davidson (Keys/Vocals), Jeffrey Davis (Guitar/Vocals)

---

## Context

The performance duo relies on custom multi-track backing assets purchased from Karaoke-Version to drive live performances and rehearsals via a Boss RC-500 Loop Station.

### Hardware & Signal Routing Baseline

The Boss RC-500 natively features independent dual stereo tracks (**Track 1** and **Track 2**) with assignable output routing:

* **Track 1:** Carries the audience-facing performance backing instrumentation (Bass + Drums/Percussion) or collaborative rehearsal mixes, routed directly to the main PA (Electro-Voice EVOLVE 30M).
* **Track 2:** Carries the synchronized reference audio (Count-in / Click), routed strictly to In-Ear Monitors (IEMs).
* **Hardware Asset Format:** The RC-500 requires uncompressed 44.1 kHz, 32-bit float or 24-bit `.wav` files. It does not accept standard `.mp3` files directly.

### Current Operational Bottleneck

The incumbent process for onboarding a single song takes **at least 30 minutes** and suffers from severe procedural bloat:

1. **Exhaustive Stem Extraction:** Manually downloading 8–14 individual MP3 stems per song from Karaoke-Version (Click, Drums, Percussion, Bass, multiple Guitars, Keys, Backing Vocals, Lead Vocals).
2. **Multi-Pass Pitch Manipulation:** For transpositions—particularly shifts beyond ±2 semitones (up to -6 semitones)—Karaoke-Version's built-in transpose algorithm introduces audible artifacts or exceeds platform limits. To preserve transient punch, the collaborator routes individual tonal stems through Anytune Pro+ one by one before bringing them into the DAW.
3. **Heavyweight DAW Reassembly:** Rebuilding the full arrangement from scratch inside Apple Logic Pro. Logic automation lanes are manually configured to silence the click track after the 1–2 measure count-in, un-muting strictly during rhythm-free breakdowns or fermatas.
4. **Dual WAV Export:** Bouncing separate `.wav` streams for RC-500 Track 1 and Track 2.
5. **Practice Mix Permutations:** Maintaining massive, multi-gigabyte Logic sessions simply to render alternate rehearsal mixes (e.g., muting guitars for Jeff or muting keys/lead vocals for Scott).

---

## Requirements

* **R1 (Dual-Track Hardware Output):** Output two synchronized, sample-accurate 44.1 kHz 32-bit float / 24-bit `.wav` files ready for direct import into RC-500 Track 1 (Audience Backing / Practice Bed) and Track 2 (IEM Click).
* **R2 (Click Automation):** IEM click track must provide a 1–2 measure count-in, silence completely during standard play, and selectively un-mute during rhythm-less breakdowns or fermatas.
* **R3 (High-Fidelity Transposition):** Support extreme key shifts (e.g., -3 to -6 semitones) using Anytune Pro+ HQ algorithms. Drum and percussion transients must remain pristine and un-pitched.
* **R4 (Two-Tier Practice Permutation Support):**
* **R4a (Tier 1 – Solo Out-of-Rig Practice):** Fast, direct `.mp3` downloads stored in Google Drive for solo ear-training and part-learning on mobile, iPad, or Mac (e.g., isolated guitar for Jeff, full mix minus keys for Scott).
* **R4b (Tier 2 – Collaborative Rig Rehearsal):** Synchronized, transposed `.wav` mixes loaded onto the RC-500 that include full band arrangements minus the live performers' instruments, complete with the IEM count-in click on Track 2.


* **R5 (Throughput Efficiency):** Reduce end-to-end processing time from **≥30 minutes** down to **<10 minutes** per song.

---

## Options Considered

### Option 1: Status Quo (Full Stem Download + Multi-Stem Anytune + Logic Pro Archival)

Continue downloading 8–14 individual stems, pitch-shifting each tonal stem individually in Anytune, rebuilding the entire mix inside Logic Pro, and maintaining large DAW archives.

* **Pros:** Complete mixing control over every isolated instrument level.
* **Cons:** Violates R5 (>30 minutes per title); creates massive disk storage bloat; highly repetitive; makes track prep dependent on maintaining complex DAW sessions.

### Option 2: Pure Web-Mixer Export (Karaoke-Version Only, Zero DAW)

Generate pre-mixed backing tracks and practice tracks directly from Karaoke-Version's mixer and pitch tools.

* **Pros:** Fastest turnaround (<2 minutes per track).
* **Cons:** **Fails R1, R2, and R3.** Karaoke-Version outputs `.mp3` rather than the `.wav` files required by the RC-500; cannot automate click track mutes over time; and introduces unacceptable phase/transient degradation on key shifts beyond 2 semitones.

### Option 3: Consolidated Stem Export + Single-Pass Anytune + Lightweight DAW Assembly (Accepted)

Pre-mix sub-stems upstream inside Karaoke-Version into three consolidated categories, perform a single Anytune transposition pass on tonal material only, and use a lightweight DAW session strictly for click truncation and `.wav` export.

* **Pros:** Satisfies R1 through R5; cuts stem downloads from ~12 to 3; runs Anytune exactly **once** instead of once per instrument; preserves pristine drum transients; handles both gig tracks and RC-500 rehearsal tracks in under 8 minutes.
* **Cons:** Requires a quick hop through Anytune for the tonal bed and 60 seconds of click trimming in a DAW.

---

## Decision

**Adopt Option 3: Consolidated Stem Export + Single-Pass Anytune + Lightweight DAW Assembly.**

We will establish a standardized ingestion protocol that decouples solo rehearsal needs from rig-ready production, utilizing upstream web sub-mixing and a single Anytune batch step:

```
[Karaoke-Version Web Mixer]
       │
       ├──► 1. Drums & Percussion (Stereo MP3, Original Key) ──────────┐
       │                                                               │
       ├──► 2. Tonal Bed (Stereo MP3, Original Key)                    │
       │         • Gig Track: Bass Only                                │
       │         • Rehearsal Track: Bass + Guitars + Aux (minus live)  │
       │         │                                                     │
       │         ▼                                                     │
       │    [Anytune Pro+ HQ] (Shift -3 to -6 Semitones)               │
       │         │                                                     │
       │         └──► Transposed Tonal WAV ────────────────────────────┼──► [DAW Template]
       │                                                               │      • Track 1: Drums + Tonal WAV -> Export Track 1 .WAV
       └──► 3. Click Track (Stereo MP3) ───────────────────────────────┘      • Track 2: Trim Click to Count-in -> Export Track 2 .WAV
                                                                                                            │
                                                                                                            ▼
                                                                                                    [Boss RC-500 Pedal]

```

### Operational Workflow

#### 1. Ingestion & Upstream Sub-Mixing (Karaoke-Version)

For any song requiring a key change, download at nominal pitch:

* **Stem 1 (Rhythm):** Solo Drums + Percussion $\rightarrow$ Download `.mp3`.
* **Stem 2 (Tonal Bed):** Mute Drums, Percussion, and Click.
* *For Gig Backing:* Solo **Bass** only.
* *For RC-500 Rehearsal Backing:* Unmute Bass, Guitars, Backing Vocals, Pads (leaving muted only the specific instruments Scott or Jeff are playing live).
* Download as a single unified `.mp3`.


* **Stem 3 (Click):** Solo Click only $\rightarrow$ Download `.mp3`.

#### 2. Single-Pass High-Fidelity Transposition (Anytune Pro+)

* Import **Stem 2 (Tonal Bed)** into Anytune Pro+.
* Apply the target semitone shift using HQ pitch mode.
* Export as a 44.1 kHz `.wav` file.
* *Advantage:* Because drums and percussion are omitted from this file, Anytune processes strictly harmonic material. No cymbal flanging, comb filtering, or transient softening occurs, and Anytune is run only **once**.

#### 3. Lightweight DAW Assembly & Truncation (Logic Pro or Audacity)

* Drag **Stem 1 (Drums)**, the **Transposed Stem 2 WAV (Tonal Bed)**, and **Stem 3 (Click)** into a 3-track template. All files align to sample zero on beat 1.
* On the Click track: highlight and slice/delete everything past the 1–2 measure count-in (leaving any designated bridge cues intact).
* Export for Boss RC-500:
* **Pedal Track 1 (Main Out):** Bounce Stem 1 + Transposed Stem 2 as a 44.1 kHz, 24-bit or 32-bit float `.wav`.
* **Pedal Track 2 (IEM Out):** Bounce the trimmed Click track as a 44.1 kHz, 24-bit or 32-bit float `.wav`.



#### 4. Tier 1 Practice Tracks (Solo Mobile/Desktop)

* For individual ear-training (e.g., Jeff learning a specific guitar solo or Scott checking a vocal harmony), use Karaoke-Version's web player directly to isolate the part, or export a single stereo mixdown directly to Google Drive as an `.mp3`. No DAW or pedal processing is required.

---

## Consequences

### Positive

* **Meets Throughput Goal (R5):** Total song ingestion time drops from ≥30 minutes to **6–8 minutes**.
* **Pristine Audio Fidelity (R3):** Extreme transpositions (-3 to -6 semitones) sound clean on stage because rhythm transients bypass pitch-shifting entirely while tonal elements receive Anytune's high-grade processing.
* **Single Anytune Hop:** Eliminates repetitive stem-by-stem pitch-shifting; Anytune processes the pre-balanced tonal bed in one pass.
* **Direct RC-500 Compatibility (R1, R4b):** Resolves the `.mp3` vs. `.wav` incompatibility on the hardware pedal for both gig and collaborative rehearsal sets.
* **Storage Optimization:** Eliminates multi-gigabyte Logic Pro project folders. Only the 3 source files and the 2 final output `.wav` files need archival.
* **Band Autonomy:** Either musician can prep a song completely for their own RC-500 without handoffs or waiting for shared project assets.

### Negative / Trade-offs

* **Baked Sub-Mix Balances:** The relative balance within the drum kit (e.g., kick-to-snare) and within the tonal rehearsal bed (e.g., guitar-to-bass) is fixed during the Karaoke-Version download. Rebalancing requires adjusting the web mixer and re-downloading that stem.
* **Dual Export for Rehearsal vs. Gig:** If a song requires both an RC-500 rehearsal mix (with rhythm guitars/backing vocals) and a gig mix (bass + drums only), Step 2 and Step 3 must be executed for each target arrangement. However, because both passes reuse Stem 1 (Drums) and Stem 3 (Click), the second pass takes less than 3 minutes.