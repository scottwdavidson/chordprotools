# chordprotools — Product Plan (LIVING DOCUMENT)

> **Owner:** Scott · **Maintained by:** whatever agent is working this repo, on Scott's direction.
> **Purpose:** A working doc for two things that deliberately do NOT mix:
> 1. **Phase 1 — Launch Readiness Review:** Scott personally uses every core
>    feature as if launching today, and stamps it approved / not approved.
>    Along the way, minor bugs and feature ideas get captured — **not fixed
>    on the spot**. Stay in reviewer mode, not refactor mode.
> 2. **Phase 2 — Roadmap:** once Phase 1 gives us an honest baseline, we use
>    the captured issues/ideas (plus new discussion) to decide where the
>    product goes next and in what order.
>
> This file is meant to be edited over many sessions. Update statuses as you
> go; don't let it go stale. Git tracks its history — no need to keep old
> superseded content around, just update in place and commit.

---

## Phase 1 — Launch Readiness Review

**How to use this:** For each row, Scott actually runs/uses the feature
(ideally against real gig data, not a toy example) and gives it one of these
verdicts. An agent's job is to help exercise the feature, capture findings,
and update the table — **not** to decide something is "fine" on Scott's
behalf.

| Verdict code | Meaning |
|---|---|
| `TODO` | Not yet reviewed (default state — leave the cell blank or use this) |
| `APPROVED` | Used it, works as expected, no notes |
| `APPROVED (issues logged)` | Works well enough to launch; issues logged below |
| `BLOCKED` | Does not work / not trustworthy for a real gig yet |

### 1.1 Catalog management

| Feature | Command(s) | Verdict | Notes |
|---|---|---|---|
| Register a new song | `./import-song <path> [--dry-run]` |  | |
| Push metadata to a single song | `./update-song <song-id>` |  | |
| Push metadata to a batch | `./update-songs` (via `updateSongsListing.txt`) |  | |
| Catalog ↔ file integrity check | `./verify-catalog` |  | |
| Cross-key-variant metadata consistency | `./consistent-metadata [--fix]` |  | |
| Strip `\r` after Sheets/Excel edit | `./tidy-song-catalog` |  | |
| Find a song's SONG ID | `./find-song-id <fragment>` |  | |
| Grep filenames | `./find-song <fragment>` |  | |

### 1.2 Gig planning

| Feature | Command(s) | Verdict | Notes |
|---|---|---|---|
| List existing gigs | `./list-gigs` |  | |
| Clone a prior gig's setlist | `./copy-gig <src> <new>` |  | |
| Strip `\r` + sort after Sheets/Excel edit | `./tidy-gigs` |  | |
| Export the fan-facing setlist | `./export-setlist [--gig <slug>]` |  | |
| Export incl. backup/Z-set songs | `./export-setlist --gig <slug> --verbose` |  | |

### 1.3 RC-500 hardware pipeline — **HIGH PRIORITY, LIKELY UNTESTED END-TO-END**

> This is the area Scott flagged as not yet properly exercised. Treat every
> row here as suspect until proven otherwise on **real hardware, at a real
> (or full-dress-rehearsal) gig.**

| Feature | Command(s) | Verdict | Notes |
|---|---|---|---|
| Assign RC-500 slot numbers for a gig | `./assign-backing-track-slots --gig <slug>` |  | |
| Generate the hardware deploy script | `./deploy-rc500 --gig <slug>` |  | |
| **Actually run the generated script against a mounted RC-500** | (manual — review/trim/run the generated `deploy-rc500-<ts>.sh`) |  | The real proof: did the right `.wav` files land in the right `0XX_1`/`0XX_2` folders? |
| Play a full gig set on the RC-500 using the deployed slots | (manual, on stage or full rehearsal) |  | Does count-in, click routing, and backing track actually work as designed in `docs/architecture/audio-workflow.md`? |
| SONG LABEL displays correctly on RC-500 hardware | (manual — check the 12-char label truncation in real life) |  | |

### 1.4 OnSong / file staging

| Feature | Command(s) | Verdict | Notes |
|---|---|---|---|
| Stage all `.cho` for OnSong | `./copyChoSetlist` |  | |
| Stage `.cho` + `.pdf` | `./copyAllSetlist` |  | |
| Stage PDFs only | `./copyPdfSetlist` |  | |
| Hand-curated per-gig copy | `./copySetlist` |  | |

### 1.5 Linting / hygiene

| Feature | Command(s) | Verdict | Notes |
|---|---|---|---|
| Directive lint (check mode) | `./lint-cho.zsh --check` |  | |
| Directive lint (fix mode) | `./lint-cho.zsh --fix` |  | |
| Bulk `{c:}` → `{comment:}` fix | `./fix-directive` / `./fix-directive-dry-run` |  | |

### 1.6 Build / tooling

| Feature | Command(s) | Verdict | Notes |
|---|---|---|---|
| Build the fat JAR | `./build` |  | Includes VPN-aware Maven settings auto-select |
| Fast command launcher + auto-rebuild | `./cpt <cmd>` |  | |
| Version reporting | `./cpt --version` |  | |
| CLI help | `./help` |  | |

---

## Issue Log (captured during Phase 1, triaged not fixed)

Add a row every time Phase 1 turns up a bug or a "this is annoying" moment.
Severity is your call — rough guide: **P2** = should fix before really
trusting it for gigs, **P3** = nice-to-have / polish.

| # | Date | Area | Severity | Description | Status |
|---|---|---|---|---|---|
| 1 | 2026-08-15 | Catalog / RC-500 metadata | P3 | `ThatThingYouDo.cho` (BACKING) and `ThatThingYouDo-a.cho` (NORD) have metadata DRIFT vs catalog — pre-existing, found by `./verify-catalog`, not yet fixed. | Open |
| 2 | 2026-08-15 | Docs | P3 | `docs/arch/**/*.puml` class diagrams reference `CatalogManagementPort`/`CatalogManagementAdapter`, which don't match the real class names in code (`CatalogPort`/`CatalogAdapter`-style). Stale early sketch, cosmetic only. | Open |
| 3 | 2026-08-15 | Tooling (agent-facing) | P2 | `import-song` does an unlocked read-modify-write append to `song-catalog.csv` — running two imports concurrently silently drops one (no error). Fine for a human running commands one at a time; a footgun for automation/agents. | Open |

*(Add more rows as Phase 1 turns them up — don't overthink numbering, just increment.)*

---

## Idea Parking Lot (raw material for Phase 2 — not prioritized yet)

Capture new ideas here the moment they occur, even mid-Phase-1. Don't debate
them yet — that's Phase 2's job.

**Already-written design docs** (real plans, not yet built):
- `stabilize-song-body-delimiter.md` — explicit header/body boundary marker in `.cho` files (fixes a duplicate-metadata class of bug).
- `consistent-song-data.md` — diff song *bodies* (not just metadata) across key-variants, transposition-aware. Depends on the delimiter work above.
- `verify-hardware` concept (in `docs/architecture/audio-workflow.md` §5–6) — SHA-256 manifest verification between Google Drive masters and what's actually on the mounted RC-500.

**Built but dangling (infrastructure exists, no command wired up):**
- RC-500 `.RC0` memory-bank read/write command (`Rc500MemoryBank` model is done; no CLI command uses it yet).
- `ChordProTransposer` — implemented + tested, not wired to any command. (`consistent-song-data` wants this; could also stand alone as a manual "transpose this chart" command.)

**New ideas surfaced during this planning session:**
- `song-characterization-and-venue-fit.md` — new song metadata (energy
  level, vocal intensity, genre primary/secondary, sing-along flag) + a
  per-gig venue policy (max vocal intensity, energy ceiling) + an
  `evaluate-setlist` validator/scorer that checks a gig's setlist against
  its venue and its energy "arc" shape. Auto-generating a setlist is
  explicitly deferred until that scorer is trusted.
  **Phase 0 (schema) shipped 2026-09-06**: `CatalogEntry`/`song-catalog.csv`
  now carry `energyLevel`, `vocalIntensity`, `genrePrimary`, `genreSecondary`,
  `singAlong` — all nullable, all 543 existing rows verified to still parse
  cleanly (`verify-catalog`: 533 clean / 10 pre-existing unrelated drift
  issues, unchanged from before this change). Fields are catalog-CSV-only
  for now, deliberately **not** echoed into `.cho` file headers.
  **Phase 1 (venue-profiles.csv) shipped 2026-09-06, refactored same day**:
  originally gig-keyed, but real usage immediately showed that was wrong —
  Moods Wine Bar needed the same policy copy-pasted across 3 gig rows, and
  Blue Vase/DeRose Winery (real prospective venues) couldn't be recorded at
  all with no confirmed gig yet, blocking Scott's actual goal of pitching
  candidate setlists to venues before a gig is booked. Refactored:
  `venue-profiles.csv` is now keyed by **venue name** (First Friday, Balboa,
  Moods Wine Bar, DeRose Winery, Blue Vase all populated with real policies),
  plus a new thin `gig-venues.csv` (gig → venue name) for the gigs that do
  have a confirmed venue (First Friday, Balboa, the 3 Moods gigs).

  **Phase 2 (`evaluate-setlist`) shipped 2026-09-06**: joins a gig's setlist
  against its resolved venue policy, reports hard vocal/energy violations,
  splits the setlist into thirds for an energy-arc summary (avg/median per
  third + trend note), and reports sing-along placement near the end.
  Degrades gracefully when data's missing (today: everything, since Phase 3
  hasn't backfilled the catalog) — shows uncharacterized counts and skips
  checks it can't run, rather than erroring. 298/298 tests pass. Smoke-
  tested against real gigs. Next up: Phase 3 (`characterize-songs` bulk
  backfill) — evaluate-setlist has nothing real to say about any gig until
  the catalog actually has energy/vocal/sing-along ratings.

  **Phase 3 (`characterize-songs` + catalog backfill) shipped 2026-09-06**:
  new command merges a ratings CSV onto `song-catalog.csv` (grouped by song
  so one rating covers every key-variant; never overwrites an already-set
  field; `--fix`/dry-run/`--verbose` mirror `bracket-chords`). Actually ran
  it: `song-ratings.csv` (checked in) has Kino's best-effort ratings for 476
  of 492 unique songs, using general music knowledge — **AI-generated, not
  authoritative, worth spot-checking against how the band actually plays
  these live**. 16 left unrated on purpose (7 Scott Davidson + 3 Michael
  Sadri originals, 6 titles Kino couldn't confidently identify) — exactly
  the "flag for manual review" behavior the design called for. 527 catalog
  rows now carry real energy/vocal/genre/sing-along data. Confirmed the
  whole pipeline works end to end: `evaluate-setlist` on First Friday now
  shows a genuine upward energy arc (4.5 → 6.5 across thirds) with 7 of 9
  sing-along songs landing in the closing third. 312/312 tests pass.

---

## Phase 2 — Roadmap

**Status: not started.** This section gets built out once Phase 1 has real
verdicts and the issue log / parking lot have enough material to prioritize
against. Structure below is a placeholder — fill in as discussions happen.

### Vision (TBD)
*What does "done" look like for this tool? Fill in after a real discussion.*

### Candidate initiatives
*Pulled from the Idea Parking Lot above once Phase 1 is far enough along to
prioritize sensibly. Will get a table with effort/impact/sequencing once
populated.*

### Discussion log
| Date | Topic | Decision |
|---|---|---|
| | | |

---

## Maintenance notes for whoever's editing this

- Update verdicts/issues in place as Phase 1 progresses — this is a living
  doc, not an append-only log. Git history is the log.
- Don't let Phase 2 content grow until Phase 1 has meaningfully progressed —
  resist the urge to roadmap while you're still supposed to be reviewing.
- When Phase 1 is fully reviewed (no `TODO` verdicts left), flip this doc's
  header to note Phase 1 is complete and Phase 2 is active.
