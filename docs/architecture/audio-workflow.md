# Audio Processing Workflow: Backing & Click Tracks
**System Documentation & Architectural Overview — `chordprotools`**

## 1. Executive Summary
The `chordprotools` audio processing module defines the ingestion, alignment, manipulation, and distribution of multi-track audio to generate performance-ready assets. The system standardizes external audio sources into two synchronized high-fidelity outputs optimized for live performance:
1. **Rhythm Accompaniment Asset (Track 2 Role):** Public-facing instrumental accompaniment routed to the main front-of-house (FOH) PA system.
2. **Isolated Timing Asset (Track 1 Role):** Internal-facing timing reference routed exclusively to the band’s In-Ear Monitors (IEMs).

By utilizing structured track boundaries, explicit count-in logic, and vocal ceiling transpositions, this workflow ensures rock-solid live synchronization while maintaining the authentic energy of a live performance.

---

## 2. Core Concepts & Operating Philosophy
To maintain consistency in future automated features or AI code assistance, development must adhere to the following core domain rules:

* **The Minimalist Accompaniment Rule:** Backing tracks are strictly limited to the foundational rhythm section—specifically **drums, percussion, and bass guitar**. Mid-to-high range melodic or harmonic elements (e.g., keyboards, lead guitars, background vocals) are intentionally excluded. The live performers bear 100% responsibility for the song's primary sonic identity and vibe.
* **Asymmetric Audio Routing:** The system outputs must support strict physical separation. The backing track is routed to the audience, whereas the click track must *only* exist in the musicians' monitors. 
* **Dynamic Click Suppression:** To prevent monitor fatigue, the click track is actively suppressed (muted) during standard playback segments where the live drums/bass provide clear temporal boundaries. The click track is dynamically unsuppressed only during:
    1. Pre-song count-ins (typically 4 or 8 beats).
    2. Arrangement "drop-outs" or quiet bridges where live rhythm instruments cease playing.
* **Target Environment Dual Modes (Gig vs. Practice):** The pipeline must accommodate two operational lifecycle phases for a song: active band rehearsal (requiring composite monaural full or partial reference mixes) and live gig execution (requiring strict dual-mono discrete routing).

---

## 3. System Architecture & Component Mapping

The implementation pipeline spans three distinct environments: Source Ingestion, DAW Engineering (including pitch modification and validation hashing), and Hardware/Cloud Execution.

```
[1. Source: Karaoke-Version] 
       │ (Download Split MP3 Stems: Rhythm Group + Pure Click)
       ▼
[2. Engineering: Anytune & Logic Pro (Mac)] 
       │ (Anytune: Pitch shift harmonic stems only)
       │ (Logic Pro: Align tracks, apply count-in pads & dynamic muting)
       │ (Export & Generate SHA-256 Manifest)
       ▼
[3. Target Ecosystem]
       ├── Google Drive (Cloud Source of Truth + SHA-256 Manifests)
       └── Deployment: BOSS RC-500 Looper
                ├── Track 1 Folder (0XX_1) ──► Right Output ──► IEM System (Timing Asset)
                └── Track 2 Folder (0XX_2) ──► Left Output  ──► House PA (Rhythm Asset)
```

| Component | Responsibility / Input | Output / Role |
| :--- | :--- | :--- |
| **Karaoke-Version API/Web** | Sourced stem files (Compressed `.mp3`). | Raw materials: Rhythm stems + explicitly padded Click stems. |
| **Anyshift / Anytune** | Selective transposition of pitched stems (e.g., Bass guitar). | Pitch-corrected `.mp3` stems matching the band's performance key. Non-pitched tracks (drums, clicks) bypass this component. |
| **Logic Pro DAW** | Track alignment, structure building, and automation. | High-fidelity master files (`.wav`) + automated SHA-256 cryptographic sidecar manifests generated upon export to Google Drive. |
| **Google Drive Storage** | Cloud repository for generated assets and verification manifests. | The definitive Source of Truth (SoT). |
| **RC-500 Looper Pedal** | Dual-track hardware playback engine. | Discretely splits physical Track 1 (Click to IEMs) and physical Track 2 (Backing to Mains). |

---

## 4. File Variants & Naming Conventions

### 4.1 Asset Lifecycle Variants
For every song managed by `chordprotools`, four distinct `.wav` assets are engineered to support the progression from initial practice to live deployment.

1. **`prx-full` (Practice Full):** A composite evaluation mix containing all available stems from the source (drums, bass, keys, guitars, vocals) along with the audible click track. Used for independent member rehearsal to learn arrangements.
2. **`prx-live` (Practice Live):** A single-track composite mix containing only the rhythm section (drums, percussion, bass) with the click track embedded directly into the track audio. Used to simulate gig-level arrangement tracking within a single audio file.
3. **Timing Performance Asset (Track 1 Role):** Isolated metronome, count-ins, and arrangement rescue lines. Formally assigned to **Track 1** of an RC-500 memory slot.
4. **Rhythm Accompaniment Asset (Track 2 Role):** Isolated rhythm section (drums, percussion, bass guitar). Formally assigned to **Track 2** of an RC-500 memory slot.

### 4.2 File System Constraints & Requirements (8.3 Naming System)
The BOSS RC-500 enforces internal file system constraints on imported tracks. To prevent truncation and ensure error-free loading, the application must adhere to a naming convention that satisfies hardware architecture while retaining human comprehensibility.

#### Hardware Display vs. Storage Layout
The hardware display relies on a 12-character, dual-line structure for memory slot labels (`SONG LABEL`), which `chordprotools` tracks using a strict 6-by-6 character split rule. However, the physical media layout on the mounted looper disk is entirely numeric and mapped to specific physical paths:

```
BOSS RC-500 (Mounted Volume)
├── ROLAND
│   ├── DATA
│   │   ├── MEMORY1.RC0   <-- Active user phrase memory / slot data mapping
│   │   ├── MEMORY2.RC0   <-- Backup / factory baseline settings
│   │   ├── SYSTEM1.RC0   <-- Global hardware configuration
│   │   └── SYSTEM2.RC0   <-- Global hardware backup
│   └── WAVE
│       ├── 001_1         <-- Memory Slot 001, Track 1 Target Folder
│       ├── 001_2         <-- Memory Slot 001, Track 2 Target Folder
│       ├── 002_1         <-- Memory Slot 002, Track 1 Target Folder
│       ├── 002_2         <-- Memory Slot 002, Track 2 Target Folder
│       └── 099_2         <-- Memory Slot 099, Track 2 Target Folder
```

#### Physical File Naming Protocol
When target tracks are compiled into their respective `0XX_Y` directories, the file names must adhere to an **8.3 format constraint**. They must remain human-discernible so that an engineer auditing the mounted drive can correlate file naming to a printed setlist.

* **Practice Mode Rules:** If a song is explicitly toggled to deploy in a rehearsal state, the system overwrites standard gig splits and names files using fixed identifiers:
  * Track 1 Folder (`0XX_1`): Must be written exactly as **`PRX-FULL.WAV`**
  * Track 2 Folder (`0XX_2`): Must be written exactly as **`PRX-LIVE.WAV`**
* **Gig Mode Rules:** When written to disk for a live performance, the file name must use an 8-character stem that visually maps to the `SONG ID` / Title while explicitly distinguishing the mix role using a standardized character placement:
  * Track 1 (Timing Role) File Pattern: Truncated 7-character token prefixed with an underscore (e.g., **`_STAYING.WAV`** or **`_DANCE.WAV`**).
  * Track 2 (Rhythm Role) File Pattern: Truncated 8-character token with no prefix (e.g., **`STAYING.WAV`** or **`DANCE.WAV`**).

---

## 5. Deployment Validation & Integrity Verification

To protect against corrupted transfers, accidental slot mismatches, or missing files during a pre-gig hardware synchronization update, a dual-layer validation workflow is required:

### 5.1 Visual Reconciliation Layer
A cross-referencing validation standard where a physical or digital gig setlist dictates specific slot assignments. The human operator must be able to visually audit the mounted file storage tree of the RC-500 and confirm that the truncated 8.3 file identifiers match the expected target repertoire.

### 5.2 Cryptographic Integrity Layer (Automated Verification)
To eliminate human error, the pipeline introduces a digital handshake verification mechanism using hashing.

1. **Manifest Generation:** Upon exporting the finalized `.wav` assets from the engineering phase to the Google Drive Source of Truth, the system automatically computes a SHA-256 hash string for each file variant. These hashes are serialized into an accompanying sidecar signature file (e.g., `manifest.sha256` or an aggregated `setlist_manifest.json`) stored in the cloud.
2. **Post-Sync Differential Audit:** After writing data to the RC-500 hardware via USB, an automated utility within `chordprotools` streams the tracks directly off the mounted looper pedal file system, calculates local runtime hashes, and diffs them against the remote Google Drive manifest. Any missing tracks, misaligned tracks, or mismatched checksums trigger immediate deployment warnings before the gear is packed for a gig.

---

## 6. System Documentation Outline

### I. Introduction & Architecture
* Scope and Intent of the Audio Module
* Component Signal Flow & Physical I/O Matrix
* Glossary of Domain Terms (`Rhythm Accompaniment Asset`, `Timing Performance Asset`, `Practice Mix`, `Cryptographic Audit`)

### II. Ingestion & Source Management (Karaoke-Version)
* Stem Selection Criteria (The Rhythm-Only Constraint vs. Full Rehearsal Stems)
* Handling Ambient Count-ins (Configuring 4-beat vs. 8-beat silence pads)
* Metadata Extraction (BPM, Native Key, Time Signature, Unique Song IDs)

### III. DAW Engineering & Mix Management (Anytune & Logic Pro)
* Pitch-shifting Pitched Stems in Anytune (Selective Transposition)
* Importing & Aligning Compressed Assets in Logic
* **The Suppressed Master Click Paradigm:** Automation rules for unmuting clicks during rhythmic drop-outs.
* Target Export Specifications (Sample rates, bit depths, mono vs. stereo configurations)

### IV. Audio Transposition & Key Modification Matrix
* Vocal Ceiling Analysis (Matching singer thresholds)
* Differentiating Pitched vs. Percussive Stem Workflows
* Pitch Shifting Artifact Mitigation

### V. Asset Deployment & Directory Mapping
* The 8.3 Filename Strategy for Physical Audio Paths
* Target Parameter Structuring for the BOSS RC-500 (Track 1 = Timing, Track 2 = Rhythm)
* Rehearsal Mode Configuration Flags (Tooling logic to toggle `prx-` files into hardware targets)

### VI. System Integrity & Automated Validation
* Manifest Compilation Pipeline on Google Drive
* Cryptographic Audit Algorithm (SHA-256 Streaming verification over USB)
* Visual Audit Protocol for Gig Reconciliations
