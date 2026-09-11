# SOP 001: Backing & Click Track Generation Runbook (Karaoke-Version $\rightarrow$ Anytune $\rightarrow$ Logic Pro $\rightarrow$ RC-500)

* **Version:** 1.0 (Draft for Field Testing)
* **Target Hardware:** Boss RC-500 Loop Station
* **Target DAW:** Apple Logic Pro X
* **Target Pitch Engine:** Anytune Pro+ (Mac)

---

## 1. Pre-Flight Preparation: Logic Pro Template Setup

*(Perform this setup once and save as a template: `File > Save as Template... > "RC500_Master_Template"`).*

1. **Project Settings:**
* File $\rightarrow$ Project Settings $\rightarrow$ Audio: Set **Sample Rate** to **44.1 kHz**.
* Drag the top tempo bar: deselect the metronome and count-in buttons so they do not sound during playback.


2. **Track Setup:**
* **Track 01:** New Audio Track. Name it `Click`. Output $\rightarrow$ `Stereo Out`.
* **Track 02:** New Audio Track. Name it `Rhythm_Drums`.
* **Track 03:** New Audio Track. Name it `Tonal_Bass`.


3. **Create the Summing Track Stack:**
* Select `Track 02` and `Track 03` simultaneously (`Shift + Click`).
* Right-click $\rightarrow$ **Create Track Stack** (or press `Shift + Command + D`).
* Select **Summing Stack**.
* Rename the main Stack Header track to **`Backing Master`**.
* Ensure the Stack Header output routes to `Stereo Out`.



---

## 2. Extraction from Karaoke-Version (KV)

Navigate to the purchased song page. Confirm the key slider is at **0 (Original Key)**.

### General Settings

* **Intro click checkbox:** **CHECKED** (Mandatory to ensure all stems share the exact same start offset).
* **Pan:** All faders centered (`C`).
* **Volume:** All active faders at 100% (nominal).

### Branching Decision:

```
Is the song being transposed to a new key?
├── NO  ──► Execute Path A (Fast-Track)
└── YES ──► Execute Path B (Transposition Pipeline)

```

### Path A: Original Key (2 Downloads)

1. **File 1 (Backing):** Click **Solo (S)** on Drums. Un-mute Percussion and Bass (plus any rhythm guitars/keys if preparing an RC-500 rehearsal mix). Click **Download MP3**.
2. **File 2 (Click):** Click **Solo (S)** on Click only. Click **Download MP3**.

### Path B: Transposed Key (3 Downloads)

1. **File 1 (Rhythm):** Click **Solo (S)** on Drums. Un-mute Percussion. Click **Download MP3**.
2. **File 2 (Tonal Bed):** Mute Drums, Percussion, and Click.
* *For Gig Track:* Solo **Bass** only.
* *For RC-500 Rehearsal Track:* Un-mute Bass, Guitars, Backing Vocals, Pads (leaving muted only what Scott or Jeff play live).
* Click **Download MP3**.


3. **File 3 (Click):** Click **Solo (S)** on Click only. Click **Download MP3**.

---

## 3. Pitch Shifting in Anytune Pro+ (Path B Only)

*(Skip for Path A).*

1. Launch **Anytune Pro+**.
2. Drag **File 2 (Tonal Bed MP3)** into the player window.
3. In the transport pitch panel (`b / #`), adjust the semitone slider to the agreed key offset (e.g., `-2.00`, `-4.00`).
4. Ensure HQ pitch mode is toggled on.
5. Go to **File $\rightarrow$ Export Tuned Song...**:
* **Range:** Whole track
* **Format:** **WAV** (Do not choose M4A/AAC to avoid generation loss)
* **Sample Rate:** 44.1 kHz


6. Save file as `Tonal_Transposed.wav`.

---

## 4. Assembly & Editing in Logic Pro

1. Open `RC500_Master_Template.logicx`.
2. Drag downloaded audio files into the project, snapping all regions hard to **Position `1 1 1 1**`:
* `Click.mp3` $\rightarrow$ `Track 01 (Click)`
* *Path A:* `Backing.mp3` $\rightarrow$ `Track 02 (Rhythm_Drums)`. Leave `Track 03` empty.
* *Path B:* `Drums.mp3` $\rightarrow$ `Track 02 (Rhythm_Drums)`, `Tonal_Transposed.wav` $\rightarrow$ `Track 03 (Tonal_Bass)`.


3. **The 2-Cut Edit:**
* Zoom in on `Track 01 (Click)` at Measure 1. Identify the count-in spikes (typically 4 or 8 clicks).
* **Cut 1 (Click Track):** Place the playhead immediately after the final count-in spike. Press `Command + T` (Split at Playhead). Select the trailing audio region (measure 3 to end of song) and hit `Delete`. *(Note: If the song features an open breakdown where the click must return, slice around that specific section and leave it intact).*
* **Cut 2 (Backing Instruments):** Select the parent **`Backing Master` Track Stack Header**. Place the playhead at the downbeat of Measure 1 of the music (immediately after the count-in spikes). Press `Command + T`. Select the leading click block on the parent Track Stack and hit `Delete`.


4. **Outro Fade & End Marker:**
* If an outro fade is needed, draw volume automation directly on the `Backing Master` channel header.
* Drag the master **Project End Marker** in the top ruler bar to sit roughly 1–2 seconds past the final instrument ring-out.



---

## 5. Bouncing for the Boss RC-500

Both files must be bounced using the exact same Cycle / End Marker range.

### Bounce 1: CLICK.WAV

1. Click **Solo (S)** on **`Track 01 (Click)`**.
2. Press `Command + B` (Bounce Project):
* **Destination:** PCM
* **File Type:** WAVE
* **Resolution:** **32-Bit Float**
* **Sample Rate:** 44.1 kHz
* **File Format:** Interleaved
* **Dither:** None
* **Normalize:** Off
* **Start:** `1 1 1 1`
* **End:** Project End Marker


3. Save filename as: `CLICK.WAV`.

### Bounce 2: BACKING.WAV

1. Un-solo Track 01. Click **Solo (S)** on the **`Backing Master`** Track Stack.
2. Press `Command + B` (Bounce Project).
3. Ensure all settings match Bounce 1 identically.
4. Save filename as: `BACKING.WAV`.

---

## 6. Hardware Loading & Sanity Check

1. Connect the Boss RC-500 to the Mac via USB and power on into **Storage Mode**.
2. Open the RC-500 drive on macOS and navigate to the target memory slot folder.
3. Copy `CLICK.WAV` into the **Track 1** directory.
4. Copy `BACKING.WAV` into the **Track 2** directory.
5. **Sanity Check:** In macOS Finder, select both files and press `Command + Option + I` (Inspector).
* Confirm both files report **44.1 kHz**, **32-bit float**, **Stereo**.
* Confirm both files report the **exact same duration down to the second and byte count**.