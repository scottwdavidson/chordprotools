# Pour Choices CLI — ChordPro Catalog Manager

A command-line tool built for the [Pour Choices band](https://pourchoicesmusic.com) to manage the
band's full library of ChordPro (`.cho`) song files.

Two CSV files are the heart of the system:

| File | Role |
|---|---|
| `song-catalog.csv` | Master song library — one row per `.cho` file, all metadata |
| `gigs.csv` | Gig assignments — which songs are in which gig, in what order, and which RC-500 slot |

They are kept deliberately separate so the catalog can be a stable, long-lived reference while setlists
change freely gig to gig without touching song metadata.

---

## Table of Contents

1. [Overview](#1-overview)
2. [Data Model](#2-data-model)
   - [song-catalog.csv](#song-catalogcsv)
   - [gigs.csv](#gigscsv)
   - [Song Versions and Key Variants](#song-versions-and-key-variants)
   - [Backing Track Devices](#backing-track-devices)
3. [Workflows](#3-workflows)
4. [Command Summary](#4-command-summary)
5. [Commands](#5-commands)
   - [import-song](#import-song)
   - [verify-catalog](#verify-catalog)
   - [consistent-metadata](#consistent-metadata)
   - [transpose](#transpose)
   - [verify-sync](#verify-sync)
   - [consistent-song-data](#consistent-song-data)
   - [update-song / update-songs](#update-song--update-songs)
   - [assign-backing-track-slots](#assign-backing-track-slots)
   - [copy-gig](#copy-gig)
   - [export-setlist](#export-setlist)
6. [Utility Scripts](#6-utility-scripts)
   - [deploy-rc500](#deploy-rc500)
7. [Repository Layout](#7-repository-layout)
8. [Technology Stack](#8-technology-stack)
9. [How to Add a New Command](#9-how-to-add-a-new-command)

---

## 1. Overview

### What the tool does

```
┌─────────────────────────────────────────────────────────────────────┐
│                         song library (.cho files)                   │
└──────────────────────────────┬──────────────────────────────────────┘
                               │  import-song
                               ▼
                       song-catalog.csv          ◄── edit in Sheets/Excel
                               │
                               │  update-song / update-songs
                               ▼
                       .cho files updated
                               │
                               │  verify-catalog
                               ▼
                   487 clean, 0 issue(s) found ✓


┌─────────────────────────────────────────────────────────────────────┐
│                           gigs.csv                                  │
│     (edit in Sheets — assign songs to gigs, set positions)          │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
           copy-gig            │  assign-backing-track-slots
      (clone prior gig)        │  (once setlist order is locked)
                               ▼
                 RC SLOT written to gigs.csv + .cho files

                               │  deploy-rc500
                               ▼
              deploy-rc500-<timestamp>.sh generated
              (review, edit, then run against RC-500)

                               │  export-setlist
                               ▼
                       setlist.csv  +  terminal table
```

The catalog and the setlist are **independent edit surfaces**. You can
re-key a song, rename an artist, or update a Nord preset in the catalog
without touching any setlist. Likewise you can rebuild a setlist from
scratch without disturbing the catalog.

RC-500 backing-track slot numbers are **per-gig assignments** stored in
`gigs.csv` — they are never a property of the song itself.

---

## 2. Data Model

<a id="song-catalogcsv"></a>

### `song-catalog.csv`

One row per `.cho` file. All permanent metadata for that specific file version lives here.

| Field | Description |
|---|---|
| `TITLE` | Song title |
| `ARTIST` | Artist or band name |
| `KEY` | Musical key of this specific `.cho` file |
| `DURATION` | Song duration (e.g. `3:30`) |
| `TEMPO` | BPM |
| `COUNTIN` | Count-in type — beat buddy, backing track, etc. |
| `BACKING` | Backing-track device: `RC` (RC-500 looper), `BB` (BeatBuddy drummer), or blank (no backing) |
| `NORD` | Voice preset on the Nord keyboard |
| `ROLAND` | Voice preset on the Roland keyboard |
| `VE` | Vocal effects preset |
| `PERFORMANCE KEY` | Key the band actually performs in (may differ from the file's `KEY`) |
| `TIME SIGNATURE` | Time signature (e.g. `4/4`) |
| `SONG ID` | Unique identifier — derived from the file path (e.g. `ABC:B:BillyJoel:PianoMan`) |
| `SONG LABEL` | RC-500 display label — shown on the guitarist's loop station screen (max 12 chars) |

> **`CHORDPRO FILENAME` is not stored in the CSV.** The `SONG ID` encodes the
> full path implicitly. The file `cho/ABC/B/BillyJoel/PianoMan.cho` maps to
> `SONG ID = ABC:B:BillyJoel:PianoMan`.

> **`RC SLOT` is not in this CSV.** Slot numbers are per-gig and live in `gigs.csv`.
> The catalog only records *which device* backs a song — not which slot.

#### SONG ID format

```
<alpha-group>:<initial>:<ArtistCamelCase>:<TitleCamelCase>
```

Examples:

| SONG ID | File |
|---|---|
| `ABC:B:BillyJoel:PianoMan` | `cho/ABC/B/BillyJoel/PianoMan.cho` |
| `DEF:E:EltonJohn:Daniel` | `cho/DEF/E/EltonJohn/Daniel.cho` |
| `ABC:B:BillyJoel:PianoMan-a` | `cho/ABC/B/BillyJoel/PianoMan-a.cho` *(key variant — see below)* |

---

<a id="gigscsv"></a>

### `gigs.csv`

One row per song-in-gig assignment. Songs appear here only when they are
assigned to a specific gig.

| Field | Description |
|---|---|
| `GIG` | Gig identifier slug, date-first (e.g. `2026-06-14-rusty-nail`) |
| `SONG ID` | Foreign key into `song-catalog.csv` — **always the base version** (no key suffix) |
| `SET` | Compound position code — encodes set letter and song position within it |
| `RC SLOT` | RC-500 slot assigned for this gig — blank until assigned, either by `assign-backing-track-slots` or by typing a number directly into the cell |

> **RC SLOT is gig-specific.** `assign-backing-track-slots` for one gig only
> touches that gig's rows — other gigs are never affected. `copy-gig` **copies
> RC SLOT forward** from source to target gig (blank stays blank, a number
> copies verbatim) — the common case is adding/dropping a song or two between
> gigs, not renumbering everything from scratch.

> **A hand-typed RC SLOT is just as valid as an algorithmically-assigned one.**
> `assign-backing-track-slots` in its default mode never overwrites a non-blank
> value in this column, no matter how it got there — see
> [assign-backing-track-slots](#assign-backing-track-slots) for the full story.

> **RC SLOT only applies to RC songs.** BeatBuddy songs (`BACKING=BB`) never get
> a slot — the BeatBuddy has its own beat selection independent of the RC-500.

#### SET code convention

| Code | Meaning |
|---|---|
| `A01` | Set A, song 1 |
| `A02` | Set A, song 2 |
| `B01` | Set B, song 1 |
| `C03` | Set C, song 3 |
| `Z01` | Backup pool, song 1 (not printed on fan setlists) |

Songs are sorted by SET code when a setlist is exported.

#### Foreign key rule

`gigs.csv` references songs by **base SONG ID only** — never a key-variant ID.
The system enforces this at read time and will throw a descriptive error if a
variant ID (e.g. `PianoMan-a`) is found in the file, telling you exactly what
to change it to.

---

### Song Versions and Key Variants

Some songs exist in multiple `.cho` files — a *base version* and one or
more *key variants* transposed to suit vocals or guitar playability:

| SONG ID | KEY | Notes |
|---|---|---|
| `ABC:B:BillyJoel:PianoMan` | C | base version — always present |
| `ABC:B:BillyJoel:PianoMan-a` | A | key variant for guitar |

**Rule:** The base version (no suffix) must always exist in the catalog. If
only a variant exists, the system will throw when attempting a setlist join.

**Why two versions exist:**

- `BACKING`, `SONG LABEL`, and all other metadata are written to **all** versions
  of a song, because the guitarist may open either `.cho` file on stage.
- `{meta: rc-slot: N (gig-name)}` is written by `assign-backing-track-slots` to
  all `.cho` versions of a song, annotated with the gig it was assigned for, so
  the slot — and which gig it's actually good for — is visible regardless of
  which file is open on the iPad. The write is skipped when the value hasn't
  changed, so re-running against an unchanged gig touches no files.
- Setlists reference only the base SONG ID — the system treats "Piano Man"
  as one song regardless of how many transposed files exist.

Use `./find-song-id` to discover SONG IDs for use in gig assignments:

```zsh
./find-song-id "piano"
```

```
TITLE                   ARTIST       KEY   SONG ID                         VARIANTS
Piano Man               Billy Joel   C     ABC:B:BillyJoel:PianoMan        +1 key variant
```

The `SONG ID` column always shows the base version — safe to paste directly into `gigs.csv`.

---

### Backing Track Devices

The `BACKING` column in `song-catalog.csv` stores the **device type**, not a slot number.

| Value | Device | RC SLOT used? |
|---|---|---|
| `RC` | RC-500 loop station — plays a pre-recorded audio backing track | Yes — assigned per gig in `gigs.csv` |
| `BB` | BeatBuddy pedal — generates a drum pattern live | No — beat is selected on the hardware |
| *(blank)* | No backing track — live acoustic or click-free | No |

The `getBacking()` accessor on a setlist entry returns:
- `"BB"` for BeatBuddy songs
- The RC-500 slot number (from `gigs.csv`) for RC songs
- `""` for no backing

---

## 3. Workflows

### Adding a new song to the library

```zsh
# 1. Drop the .cho file into the right cluster directory:
#    cho/<alpha-group>/<initial>/<ArtistCamelCase>/TitleCamelCase.cho
#
#    Path uses the simplified form; metadata can be richer:
#      {title: Movin' Out (Anthony's Song)}  ← full title; path just uses MovingOut
#      {artist: Billy Joel}                  ← primary artist; path matches
#      {key: A}                              ← required; OnSong needs it explicit
#
# 2. Import it — SONG ID is derived from the path automatically:
./import-song cho/ABC/B/BillyJoel/MovingOut.cho --dry-run   # preview first
./import-song cho/ABC/B/BillyJoel/MovingOut.cho             # add to catalog
#
# 3. Open song-catalog.csv in Google Sheets, fill in remaining metadata
# 4. Save CSV → tidy → push metadata back to the file (by SONG ID):
./tidy-song-catalog
./update-song ABC:B:BillyJoel:MovingOut
#
# 5. Confirm catalog and file agree:
./verify-catalog
```

### Updating song metadata

```zsh
# Edit the row in song-catalog.csv in Sheets, then:
./tidy-song-catalog
./update-song ABC:B:BillyJoel:PianoMan   # or use ./find-song-id PianoMan to get the song ID
./verify-catalog
```

### Planning a new gig

```zsh
./list-gigs                                                   # see existing gigs + song counts
./copy-gig 2026-05-10-rusty-nail 2026-06-14-rusty-nail        # clone a prior gig as a starting point (RC SLOT copies forward too)
# open gigs.csv in Sheets
# reorder SET codes, swap songs as needed
./tidy-gigs                                                   # required after any Sheets/Excel save
./export-setlist --gig 2026-06-14-rusty-nail                  # preview the fan setlist (no Z-sets)
./export-setlist --gig 2026-06-14-rusty-nail --verbose        # preview with backup songs too
```

### Locking in a gig (finalising backing-track slots)

Run `assign-backing-track-slots` once the setlist is settled enough to load the
RC-500. It's safe to run early and often — by default it never touches a slot
that's already there (whether `copy-gig` carried it forward or you typed it in
by hand), it only fills in blanks for songs that don't have one yet. Adding or
dropping a song later and running it again just fills in the new gaps.

```zsh
./assign-backing-track-slots --gig 2026-06-14-rusty-nail
# → Existing RC SLOT values in gigs.csv (this gig only) are left untouched
# → Blank RC SLOT values get the next free number (gaps filled before extending upward)
# → {meta: rc-slot: N (2026-06-14-rusty-nail)} synced into each affected .cho file
# → setlist.csv regenerated
```

Want a genuine from-scratch renumber instead (e.g. the set order changed enough
that the old numbers no longer make sense)? Add `--reoptimize`:

```zsh
./assign-backing-track-slots --gig 2026-06-14-rusty-nail --reoptimize
```

Once slots are assigned, generate the RC-500 deploy script:

```zsh
# Generate for the latest gig (paths from application.properties):
./deploy-rc500

# Generate for a specific gig:
./deploy-rc500 --gig 2026-06-14-rusty-nail

# Override paths at the command line:
./deploy-rc500 --gig 2026-06-14-rusty-nail \
    --source /Volumes/G-DRIVE/BackingTracks \
    --target /Volumes/RC-500
```

This generates a `deploy-rc500-<timestamp>.sh` script you can review, trim
(e.g. just the songs for tonight’s practice), then run.

See [deploy-rc500](#deploy-rc500) for the full command reference.

### Adding songs to an existing gig

```zsh
./find-song-id "joel"              # find the SONG ID for the song you want
# paste the SONG ID + gig + SET code into gigs.csv in Sheets
./export-setlist --gig <gig-slug>  # verify the setlist looks right
```

---

## 4. Command Summary

| Script | CLI Subcommand | Description |
|---|---|---|
| `./import-song` | `import-song` | Register a new `.cho` file in `song-catalog.csv` (SONG ID derived from file path) |
| `./verify-catalog` | `verify-catalog` | Check every `song-catalog.csv` entry against its `.cho` file; report MISSING FILE or DRIFT |
| `./consistent-metadata` | `consistent-metadata` | Check key-variants of a song share consistent metadata (all but key); `--fix` to repair drift |
| `./transpose` | `transpose` | Create a new key-variant of a song from its SONG ID: transposes, derives the output filename automatically, and (by default) catalogs it |
| `./verify-sync` | `verify-sync` | Compare any two `.cho` files for semantic drift (lyrics/chords) that survives pure transposition |
| `./consistent-song-data` | `consistent-song-data` | Check that a song's key-variants share the same lyrics and harmonic content, not just metadata |
| `./update-song` | `update-song` | Push catalog metadata into a song (by song ID) and all its key-variants |
| `./update-songs` | `update-songs` | Push catalog metadata into a batch of songs (by song ID) |
| `./assign-backing-track-slots` | `assign-backing-track-slots` | Fill in RC-500 slot numbers for the gig, preserving whatever's already in `gigs.csv` by default; `--reoptimize` to recompute everything from scratch |
| `./copy-gig` | `copy-gig` | Clone all gig assignments (including RC SLOT) from one gig slug to a new one |
| `./export-setlist` | `export-setlist` | Join catalog + assignments and export a gig-ready `setlist.csv` |
| `./find-song-id` | `find-song-id` | Search the catalog by title/artist fragment → SONG ID to paste into `gigs.csv` |
| `./list-gigs` | `list-gigs` | List every gig slug in `gigs.csv` with a song count |

Quick help at any time:

```zsh
./help
```

### How commands run (build once, run fast)

The Java-backed command shims do **not** run `mvn spring-boot:run` on every
invocation (which adds ~5–10s of Maven overhead each time). Instead they
delegate to an internal launcher, **`cpt`**, which runs the packaged fat JAR
directly with `java -jar` — typically under **1 second**.

| Script | Purpose |
|---|---|
| `./build` | Compile + package the fat JAR (`mvn package -DskipTests`). Run manually any time; also called automatically by `cpt`. |
| `./cpt <command> [args]` | Internal launcher. Runs the JAR directly; **auto-rebuilds** it whenever it's missing or out of date, so the code you run always matches the code on disk. |

You rarely call these directly — every command shim (`./export-setlist`,
`./deploy-rc500`, …) delegates to `./cpt`. The flow:

```
./deploy-rc500 --gig …
   └─► ./cpt deploy-rc500 --gig …
          ├─ JAR missing or out of date?  → ./build, then run
          └─ up to date                   → java -jar … (fast path)
```

**Never goes stale.** “Out of date” means any file under `src/main`, or
`pom.xml`, is newer than the JAR. So after a plain `git pull` that changes
code, resources, or dependencies, the very next command rebuilds
automatically — nobody has to remember to run `./build`, and you can never
accidentally run yesterday's code. (Rebuild messages go to stderr, so piped
or redirected command output stays clean.)

> **Note:** `tidy-*`, `fix-*`, `copy*Setlist`, `lint-cho.zsh`, and `find-song`
> are pure shell — none of these touch the JAR. (All the catalog/gig commands,
> including `find-song-id` and `list-gigs`, are Java and go through `cpt`.)

---

## 5. Commands

### `import-song`

**Script:** `./import-song <path-to-cho-file> [--dry-run]`

Registers a new `.cho` file in `song-catalog.csv`. The SONG ID is derived
automatically from the file path — you never construct it manually.

```zsh
# Preview what would be added, without modifying the catalog:
./import-song --dry-run cho/ABC/B/BillyJoel/MovingOut.cho

# Add the song to the catalog:
./import-song cho/ABC/B/BillyJoel/MovingOut.cho
```

#### What the `.cho` file needs at import time

Three directives are **required** before importing:

| Directive | Why |
|---|---|
| `{title: ...}` | Catalog display, setlist output, `find-song-id` search |
| `{artist: ...}` | Same — a row without an artist is a ghost in the catalog |
| `{key: ...}` | OnSong derives the key from the first chord if absent — always set it explicitly |

Everything else (tempo, duration, hardware presets, label) can be left blank
and filled in later via Google Sheets → `./tidy-song-catalog` → `./update-song`.

#### Path vs. metadata — simplified vs. full

The file **path** is a compact, filesystem-safe identifier. The **directives**
are the human-readable form and can carry more detail. The two don't need to
be identical — they just need to refer unambiguously to the same song.

**Artist:** use the primary/headline artist in the path. The `{artist:}`
directive can include the full credit.

```
cho/ABC/B/BrunoMars/UptownFunk.cho
  {artist: Bruno Mars ft. Mark Ronson}   ← full credit in metadata
  ↑ path uses primary artist only
```

**Title:** use the standard short name in the path. The `{title:}` directive
can include a parenthetical or subtitle that would make the filename unwieldy.

```
cho/STU/S/Supertramp/TheLogicalSong.cho
  {title: The Logical Song (What Are We)}   ← full title in metadata
  ↑ path uses the recognisable short form
```

#### Minimum viable `.cho` file

```
{title: Movin' Out (Anthony's Song)}
{artist: Billy Joel}
{key: A}
```

Guards:
- Throws if the `.cho` file does not exist
- Throws if the derived SONG ID already exists in the catalog

After importing, open `song-catalog.csv` in Google Sheets to fill in the
remaining metadata, then run `./tidy-song-catalog` and `./update-song` to
push it back into the `.cho` file.

---

### `verify-catalog`

**Script:** `./verify-catalog`

Reads every row in `song-catalog.csv`, opens the corresponding `.cho` file,
and compares their metadata field by field. Reports two classes of issues:

| Issue | Meaning |
|---|---|
| `MISSING FILE` | The `.cho` file referenced by a catalog row does not exist on disk |
| `DRIFT` | One or more fields in the catalog row do not match the `.cho` header |

```zsh
./verify-catalog
# → verify-catalog: 487 clean, 0 issue(s) found
# → All catalog entries match their .cho files. ✓
```

Use `verify-catalog` as a sanity check after any bulk operation (e.g. after
running `update-songs` or after manually editing `.cho` files). Note that
`RC SLOT` is intentionally excluded from the comparison — it is a per-gig
assignment owned by `gigs.csv`, not a song property.

---

### `consistent-metadata`

**Script:** `./consistent-metadata [--fix] [--source <song-id>]`

A periodic "tidy" check that key-variants of the **same song** agree on their
metadata. Two key-variants (e.g. `HollywoodNights.cho` in E and
`HollywoodNights-b.cho` in B) are the same song in different keys, so all their
catalog metadata should be identical — the *only* legitimate difference is
the written **key**.

> **Performance key is an invariant.** The performance key is the *sounding*
> key everyone actually plays in, so it must match across variants. Example:
> the standard variant is written in key **C**; the guitarist's
> variant is written in **B♭**. Both sound in **C**, so the
> performance key is **C** for both. Only `key` may differ.

It scans the whole catalog and reports two classes of issue:

| Issue | Meaning |
|---|---|
| `DRIFT` | A field other than KEY differs between variants (incl. performance key) |
| `FILENAME/KEY` | A variant's filename key suffix (e.g. `-b`) disagrees with its `{key:}` (enharmonic-aware) |

```zsh
./consistent-metadata
# → [DRIFT] ABC:B:BobSeger:HollywoodNights
# →   ABC:B:BobSeger:HollywoodNights  vs  ABC:B:BobSeger:HollywoodNights-b
# →       COUNTIN  '8'  vs  ''
# → consistent-metadata: 42 group(s) with variants, ... , 19 issue(s)
```

**Dry-run by default.** Exits with the number of issue groups (CI-friendly).

**`--fix`** repairs `DRIFT` by copying the shared metadata from a source-of-
truth variant into `song-catalog.csv` (then run `update-song <groupId>` to push
it into the `.cho` files — it fans out to all variants). The source defaults to
each group's base (standard-key) variant; override with `--source <song-id>`.

```zsh
./consistent-metadata --fix
./consistent-metadata --fix --source ABC:B:BobSeger:HollywoodNights-b
```

> `FILENAME/KEY` issues are **never auto-fixed** — renaming a file or rewriting
> a key is a human decision (just do it in git). They are report-only.

#### ⚠️ Before you run `--fix` — read this

`--fix` copies the shared fields **from the source variant to the others**. By
default the source is the **base** (standard-key) variant. This creates one
important footgun:

> **The "empty base" trap.** If the base variant is *missing* a field that a
> key-variant *has* (e.g. the base has no performance key but `-g` does), the
> default `--fix` will copy the base's **empty** value over the variant's good
> data — wiping it. When the drift report shows `PERF KEY '' vs 'G'` (empty on
> the base, populated on the variant), do **not** blindly `--fix`. Either fix
> the base by hand first, or run `--fix --source <the-populated-variant>` so the
> good value wins.

Recommended workflow:

1. **Commit first.** `--fix` rewrites `song-catalog.csv`; git is your undo.
2. **Read the dry-run** and bucket the findings:
   - *Base is correct* → safe to `--fix` (default source).
   - *Base is empty / variant is correct* → `--fix --source <variant-id>`.
   - *Both look wrong* (e.g. a malformed value like `Bb (+3)` in a field) → fix
     by hand in the spreadsheet.
3. **`--fix`** the safe ones.
4. **Push to the files:** `./update-song <groupId>` for each fixed song — it
   fans out the corrected metadata to the base file *and* all key-variants.
5. **Re-run** `./consistent-metadata` to confirm it's clean.

> **Run order matters.** `consistent-metadata` operates on `song-catalog.csv`.
> If you've edited the catalog in a spreadsheet, run `./tidy-song-catalog`
> first. After `--fix`, run `./update-song` (not the other way round) so the
> `.cho` files receive the corrected catalog values.

---

### `transpose`

**Script:** `./transpose <SONG_ID> --offset <semitones> [--no-import]`

Creates a **new key-variant** of a song: resolves the source `.cho` file from
its SONG ID (see `./find-song-id`), transposes it, and derives the output
filename/SONG ID automatically from the naming convention — you never name
an output path yourself, and the command never overwrites the input or an
existing file.

```zsh
./transpose ABC:B:BillyJoel:MovinOut --offset 2
# → Transposed ./cho/ABC/B/BillyJoel/MovinOut.cho: Dm -> Em (+2 semitones) -> ./cho/ABC/B/BillyJoel/MovinOut-em.cho
# → Imported as SONG ID: ABC:B:BillyJoel:MovinOut-em
```

By default the new variant is **also registered in `song-catalog.csv`** —
that second line is a real catalog import, not just a file write. Pass
`--no-import` to write only the `.cho` file:

```zsh
./transpose ABC:B:BobSeger:HollywoodNights --offset -2 --no-import
# → Transposed ./cho/ABC/B/BobSeger/HollywoodNights.cho: E -> D (-2 semitones) -> ./cho/ABC/B/BobSeger/HollywoodNights-d.cho
# → Catalog import skipped (--no-import). Run ./import-song ./cho/ABC/B/BobSeger/HollywoodNights-d.cho to add it.
```

You can also transpose from an existing key-variant, not just the base file —
the new variant lands in the same song group either way:

```zsh
./transpose ABC:B:BobSeger:HollywoodNights-b --offset 2
# → new variant is still grouped under ABC:B:BobSeger:HollywoodNights
```

**Guardrails, before anything gets written:**
- **Duplicate-key guard** — refuses to create a variant in a musical key that
  already exists anywhere in the song's group, even under a different
  filename or a different enharmonic spelling (e.g. it catches a `C#`
  catalog entry when the computed target spells the same pitch `Db`).
- **Overwrite guard** — refuses to write over an existing target file.
- **Import-failure handling** — if the transpose itself succeeds but the
  catalog step fails afterward, the `.cho` file is **left in place** (never
  rolled back) and the error tells you the exact `./import-song <path>`
  command to finish the job by hand.

**What the transposition itself handles:**
- Root notes and slash-chord bass notes (`[A/E]` up 2 semitones → `[B/F#]`)
- Correct sharp/flat spelling based on the *target* key (transposing into
  Bb major produces `Eb`, not `D#`)
- Non-chord brackets — section labels (`[Bridge]`, `[Chorus]`), riff notation
  (`[E F# G A]`), and fret-position hints (`[C (17th - 3-6)]`) all pass
  through untouched

**Regression guardrail:** if a bracket looks like it was *meant* to be a
chord (starts with a note letter) but doesn't parse as one, `transpose`
prints a warning instead of silently leaving it — this is how typos and bad
copy-pastes get caught instead of lurking in the catalog forever:

```zsh
./transpose ABC:X:SomeArtist:SomeBadSong --offset 2
# → WARNING: body line 6 looks like a chord but wasn't recognized (left untransposed): [Gmjaj7]
```

(Warnings print to stderr, everything else to stdout — pipe/redirect either
independently.)

> **Known limitation:** chords with a literal `/` inside the extension
> itself (not a bass note), like `[C6/9]`, aren't recognized as a single
> chord and are left untouched — a deliberate tradeoff to avoid false
> positives elsewhere.
>
> **Need the old file-in/file-out behavior** (e.g. transposing to a scratch
> file outside the catalog)? That lower-level engine still exists
> (`TransposeService`) but isn't exposed as its own CLI command — `transpose`
> is SONG-ID-only by design, to keep the naming convention enforced rather
> than optional.

---

### `verify-sync`

**Script:** `./verify-sync <fileA.cho> <fileB.cho>`

Compares any two `.cho` files for **semantic drift** — differences in lyrics
or harmony that survive a pure transposition. Catalog-agnostic: takes two raw
file paths, no SONG ID lookup needed, so it works on any two files (a real
reason this exists: catching the day someone edits one key-variant of a song
and forgets the other).

Two independent checks:
- **Lyric drift** — strips out chords/directives, normalizes whitespace,
  compares the remaining text line-by-line.
- **Harmonic drift** — converts every chord's root to a Roman-numeral scale
  degree relative to *that file's own* key, and compares the degree
  sequences. This is what makes a pure transposition (same song, different
  key) or an enharmonic respelling (`A#` vs `Bb`) never a false positive —
  only a genuine chord *substitution* is flagged.

```zsh
./verify-sync cho/ABC/B/BobSeger/HollywoodNights.cho cho/ABC/B/BobSeger/HollywoodNights-b.cho

# → verify-sync: cho/ABC/B/BobSeger/HollywoodNights.cho  vs  cho/ABC/B/BobSeger/HollywoodNights-b.cho
# → No drift detected.
```

When something's actually wrong:

```zsh
./verify-sync cho/ABC/B/BobSeger/HollywoodNights.cho /tmp/HollywoodNights-broken.cho

# → verify-sync: cho/ABC/B/BobSeger/HollywoodNights.cho  vs  /tmp/HollywoodNights-broken.cho
# → [LYRIC] line 11: File A: "She stood there bright as the sun on that California coast."  |  File B: "She stood there bright as the sun on that Californian coast."
# → [HARMONIC] line 11: File A has IV (A), File B has bVII (D)
# → verify-sync: 2 issue(s) found.
```

`verify-sync` exits with the number of findings (0 = clean, CI-friendly).

> **Known v1 simplification:** the harmonic check compares scale-degree
> only, not the full chord extension — two chords with the same root and a
> different extension on the same degree (e.g. `Cmaj7` vs `C7`) won't be
> flagged by this check alone.

---

### `consistent-song-data`

**Script:** `./consistent-song-data <song-id>`

The catalog-aware sibling of `verify-sync` — and the *content* counterpart to
`consistent-metadata` (which only checks catalog fields, never chords or
lyrics). Resolves every key-variant of a song from `song-catalog.csv`, picks
the base (standard-key) variant as the reference, and runs `verify-sync`'s
engine against each other variant. Works from *any* variant's SONG ID —
asking about `HollywoodNights-b` checks the same group as asking about the
base `HollywoodNights`.

```zsh
./consistent-song-data ABC:B:BobSeger:HollywoodNights
# → --- ./cho/ABC/B/BobSeger/HollywoodNights.cho  vs  ./cho/ABC/B/BobSeger/HollywoodNights-b.cho ---
# → No drift detected.
# → consistent-song-data: 1 variant(s) checked, 0 issue(s) total.
```

A song with no key-variants reports cleanly (nothing to check, not an error):

```zsh
./consistent-song-data DEF:E:EltonJohn:RocketMan
# → No key-variants to compare - nothing to check. 
```

Exits with the total number of findings across every variant. **Detection
only** in this phase — no `--fix` yet (mutating `.cho` files needs more care
than a metadata copy; see the design doc for why it's deferred).

---

<a id="update-song--update-songs"></a>

### `update-song` / `update-songs`

**Scripts:** `./update-song <song-id>`, `./update-songs`

Reads metadata from `song-catalog.csv` and writes it back into `.cho` file
headers. This is the primary way catalog edits flow back into the song files.

A song is identified by its **song ID** (e.g. `ABC:B:BillyJoel:PianoMan`), not
a file path. Because song metadata (duration, count-in, tempo, …) is shared
across key-variants, a single `update-song` invocation fans out to the base
file **and every key-variant** in the same song group — change the duration
once and it lands in all of them.

> **Limitation:** because the argument is a song ID and not a file path, shell
> **tab-completion no longer works** for it. Use `./find-song-id <fragment>`
> to look up the song ID you need (it prints the SONG ID column, and annotates
> songs that have alternate-key variants).

> `update-song` preserves any `{meta: rc-slot: N (gig-name)}` already in the
> file — a slot assigned by `assign-backing-track-slots` is never erased by a
> catalog update.

**Single song:**

```zsh
./update-song ABC:B:BillyJoel:PianoMan

# Find a song's ID first if needed:
./find-song-id PianoMan
# → PianoMan | Billy Joel | C | ABC:B:BillyJoel:PianoMan (+2 variants)
```

**Batch:**

```zsh
# Edit updateSongsListing.txt — one song ID per line
./update-songs
```

---

### `assign-backing-track-slots`

**Script:** `./assign-backing-track-slots [--gig <slug>] [--output <path>] [--reoptimize]`

Assigns RC-500 backing-track slot numbers for a gig's setlist, then propagates
those numbers into `gigs.csv` and directly into the individual `.cho` files.

**Default mode preserves and fills** — the mode you want almost all the time:

1. Loads `song-catalog.csv` + `gigs.csv` and resolves the target gig
2. Any RC-backed song that **already has a non-blank RC SLOT** for this gig —
   whether set by a previous run of this command, or typed straight into the
   spreadsheet by hand — is **left completely untouched**
3. Splits the remaining (blank) songs into **in-set** (SET prefix A–Y, sorted
   by SET code) and **backup** (SET prefix Z, sorted alphabetically by title)
4. Assigns numbers to those blank songs only — in-set starting at slot **5**,
   backup starting at slot **50** — filling any gap in the existing numbers
   before extending upward
5. Writes the full set of slot values (preserved + newly assigned) for this
   gig into `gigs.csv` (**only this gig's rows are touched**)
6. Syncs `{meta: rc-slot: N (gig-name)}` into every affected `.cho` file —
   skipping the write entirely for any file whose value is already correct,
   so re-running against an unchanged gig touches zero files
7. Writes a fresh `setlist.csv`

```zsh
./assign-backing-track-slots                              # latest gig, preserve mode
./assign-backing-track-slots --gig 2026-06-14-rusty-nail  # specific gig, preserve mode
```

**Sample output:**

```
Backing-track slot sync complete for gig '2026-06-14-rusty-nail' — kept 28 existing slot(s), assigned 3 new slot(s), written to ./setlist.csv

SET     TITLE                                     ARTIST                     KEY     SLOT
-----------------------------------------------------------------------------------------------
A01     Starting Over                             Chris Stapleton            Bb      5
A02     Against the Wind                          Bob Seger                  F       6
...
```

**Want a genuine from-scratch renumber instead?** Pass `--reoptimize` to ignore
whatever's currently in `gigs.csv` for this gig and recompute every slot from
scratch, exactly like the old always-recompute behavior:

```zsh
./assign-backing-track-slots --gig 2026-06-14-rusty-nail --reoptimize
```

> **Guard-rail:** in default (preserve) mode, if `gigs.csv` has ambiguous data
> for this gig — a non-numeric or out-of-range (1–99) RC SLOT, or two
> different songs claiming the same slot — the command **aborts with nothing
> written at all** (not `gigs.csv`, not any `.cho` file, not `setlist.csv`) and
> reports exactly which songs/values need a human look. Fix the flagged rows
> in `gigs.csv` and re-run.

> **BeatBuddy songs** (`BACKING=BB`) are included in the setlist but skipped
> during slot assignment — BeatBuddy beat selection is done on the pedal itself.

> **Songs with no backing** (`BACKING` blank) are likewise skipped. If a song's
> `BACKING` type changes away from `RC`, any stray RC SLOT value left over in
> `gigs.csv` for it is cleared automatically.

> Slots 1–4 are intentionally left free. Slots 50–99 are reserved for backup songs.

> Assignments for **other gigs are never touched**. Each gig maintains its own
> independent set of slot numbers.

---

### `copy-gig`

**Script:** `./copy-gig <source-gig> <target-gig> [--force]`

Clones all setlist assignments from an existing gig to a new gig slug,
**including RC SLOT values** — a blank stays blank, a number copies verbatim.

```zsh
# Start next month's gig from last month's setlist, RC slots and all
./copy-gig 2026-05-10-rusty-nail 2026-06-14-rusty-nail

# Re-clone over a target you have already started editing
./copy-gig 2026-05-10-rusty-nail 2026-06-14-rusty-nail --force
```

Guard-rails:
- Source gig must exist in `gigs.csv` — throws if not found
- Target gig must not already have assignments unless `--force` is passed

After cloning, open `gigs.csv` in Google Sheets, adjust SET codes to reorder
songs, and swap in different SONG IDs where the set list differs from the prior
gig. Then run `./tidy-gigs` (required after any Sheets/Excel save) and
`./assign-backing-track-slots --gig <target-gig>` — in its default mode it will
fill in slots only for whatever you added, leaving every copied-forward slot
exactly as it was. Pass `--reoptimize` there instead if you'd rather renumber
everything from scratch.

---

### `export-setlist`

**Script:** `./export-setlist [--gig <slug>] [--output <path>] [--verbose]`

Joins `song-catalog.csv` and `gigs.csv` for a specific gig, sorts by SET code,
and writes `setlist.csv`. Also prints a formatted summary table to the terminal.

**By default, only fan-facing songs (SET prefix A–Y) are included** — this is
the setlist to print on paper and send to fans. Use `--verbose` to include the
Z-set backup pool as well.

```zsh
# Fan setlist only (default) — most recent gig
./export-setlist

# Full list including backup songs
./export-setlist --verbose

# Specific gig, fan setlist
./export-setlist --gig 2026-06-14-rusty-nail

# Specific gig, full list, custom output path
./export-setlist --gig 2026-06-14-rusty-nail --verbose --output ./gig-2026-06-14.csv
```

**Terminal output example:**

```
Setlist export complete — 32 songs for gig '2026-06-05-FF' written to ./setlist.csv
  (backup / Z-set songs excluded — use --verbose to include them)

SET     TITLE                                     ARTIST                     KEY     BACKING
-----------------------------------------------------------------------------------------------
A01     Starting Over                             Chris Stapleton            E       5
A02     Against the Wind                          Bob Seger                  G       6
A03     Diamond Girl                              Seals & Crofts             D       7
...

  BACKUP / Z-SET                     ← only shown with --verbose
SET     TITLE                                     ARTIST                     KEY     BACKING
-----------------------------------------------------------------------------------------------
Z01     And We Danced                             The Hooters                G       50
```

The `BACKING` column shows:
- A slot number for RC-500 songs (from this gig's `gigs.csv` RC SLOT)
- `BB` for BeatBuddy songs
- blank for songs with no backing track

> `export-setlist` will throw if any assigned SONG ID is not found in
> `song-catalog.csv`. A missing base version is treated as a data integrity
> error, not a silent skip.

---

## 6. Utility Scripts

Most scripts are thin shell shims that invoke the Java CLI application.
`deploy-rc500` is a full Java-backed command that generates a throwaway script
rather than performing copies directly.

### `deploy-rc500`

**Script:** `./deploy-rc500 [OPTIONS]`
**Java command:** `deploy-rc500` → `GenerateRc500DeployScriptCommand` / `GenerateRc500DeployScriptService`

Generates a timestamped, human-editable shell script containing plain `cp`
commands to copy `backing.wav` and `click.wav` files from the local library
to the RC-500 looper pedal. Run this after `assign-backing-track-slots` has
locked in slot numbers for the gig.

The generated script is intentionally simple — just `cp` commands with
comments — so you can open it in any editor, remove songs you don’t need
(e.g. for a partial practice load), then run what’s left.

```zsh
# Generate for the latest gig (paths read from application.properties):
./deploy-rc500

# Specific gig:
./deploy-rc500 --gig 2026-06-14-rusty-nail

# Override source/target paths at the command line:
./deploy-rc500 --gig 2026-06-14-rusty-nail \
    --source /Volumes/G-DRIVE/BackingTracks \
    --target /Volumes/RC-500

# Write the generated script to a specific directory:
./deploy-rc500 --output-dir ~/Desktop
```

#### Options

| Option | Description |
|---|---|
| `--gig` / `-g` | Gig slug. Defaults to the lexicographically latest gig in `gigs.csv`. |
| `--source` / `-s` | Root of the local backing-track library. Overrides `application.properties`. |
| `--target` / `-t` | RC-500 mount point / root directory. Overrides `application.properties`. |
| `--output-dir` / `-o` | Where to write the generated script (default: current directory). |

#### Source path layout

```
<source>/<CLUSTER>/<LETTER>/<Artist>/<SongTitle>/
  backing.wav    ← required
  click.wav      ← optional
```

Key-variant suffixes are stripped automatically — `SongId.getTitle()` already
holds the base title, so `BillyJoel:YouMayBeRight-g` → folder `YouMayBeRight/`
with no extra logic required.

#### Target path layout (standard RC-500 WAVE structure)

```
<target>/ROLAND/WAVE/
  <NNN>_1/backing.wav    ← NNN = RC slot zero-padded to 3 digits
  <NNN>_2/click.wav
```

Example: slot `7` → `007_1/backing.wav` and `007_2/click.wav`.

> **No `mkdir -p`.** The target directory structure is expected to already
> exist (either the live RC-500 or a pre-built local test mirror).
> The generated script contains only `cp` commands.

#### Generated script behaviour by case

| Situation at generation time | What appears in the script |
|---|---|
| `backing.wav` found | Live `cp` command |
| `backing.wav` missing | `⚠ WARNING` comment block; `cp` is commented out with the expect|
| `click.wav` found | Live `cp` command |
| `click.wav` missing | `# INFO` comment; line omitted (normal for some songs) |
| Song has no RC slot | Skipped entirely (not yet assigned — run `assign-backing-track-slots` first) |

#### Generated script format

```sh
#!/bin/zsh
# ===============================================================
# RC-500 Deploy Script
# Generated : 2026-06-14T09:15:03
# Gig       : 2026-06-14-rusty-nail
# Songs     : 32 RC-slotted assignment(s)
# Source    : /Volumes/G-DRIVE/BackingTracks
# Target    : /Volumes/RC-500
# ===============================================================
# Edit before running — copy only the songs you need.
# Run: ./deploy-rc500-20260614-091503.sh
# ===============================================================

# ── ChrisStapleton / StartingOver  [slot 005 / set A03] ─────────
cp "/Volumes/G-DRIVE/.../StartingOver/backing.wav" \
   "/Volumes/RC-500/ROLAND/WAVE/005_1/backing.wav"
cp "/Volumes/G-DRIVE/.../StartingOver/click.wav" \
   "/Volumes/RC-500/ROLAND/WAVE/005_2/click.wav"

# ── BobSeger / AgainstTheWind  [slot 006 / set A04] ──────────
cp "/Volumes/G-DRIVE/.../AgainstTheWind/backing.wav" \
   "/Volumes/RC-500/ROLAND/WAVE/006_1/backing.wav"
# INFO: No click.wav found for BobSeger / AgainstTheWind — omitted

# ── SealsCrofts / DiamondGirl  [slot 007 / set A05] ─────────
# ⚠ WARNING: backing.wav NOT FOUND at generation time
# Expected : /Volumes/G-DRIVE/.../DiamondGirl/backing.wav
# Uncomment once the file is in place:
# cp ".../DiamondGirl/backing.wav" \
#    "/Volumes/RC-500/ROLAND/WAVE/007_1/backing.wav"
```

#### Configuration

Set the source and target paths in `application.properties` (committed,
but left blank by default since paths are machine-specific):

```properties
chordprotools.backing-source-root=/Volumes/G-DRIVE/BackingTracks
chordprotools.rc500-target-root=/Volumes/RC-500
```

Or pass them directly with `--source` / `--target` for one-off runs or
when testing against a local directory instead of the mounted pedal.

> Generated scripts are gitignored (`deploy-rc500-*.sh`) — they are
> throwaway artifacts, not source code.

### `find-song-id`

**Java command** (`find-song-id` → `FindSongIdCommand` / `FindSongIdService`).
Search `song-catalog.csv` by title or artist fragment. Prints one row per song
(base version only), annotating how many key variants exist. The `SONG ID`
column is always a valid base ID safe to paste into `gigs.csv`.

```zsh
./find-song-id "piano"

TITLE         ARTIST       KEY   SONG ID                   VARIANTS
Piano Man     Billy Joel   C     ABC:B:BillyJoel:PianoMan  +1 key variant

./find-song-id "joel"
# → all Billy Joel songs
```

Key-variant grouping reuses `SongId.toGroupKey()` from the domain model (the
same rule the rest of the app uses), so there is no duplicated regex. If a song
exists only as a key variant with no base version in the catalog, it is flagged
`[!]` as an orphan — a data-integrity issue to fix before using the ID in a
setlist. Exits with code 1 if nothing matches.

### `list-gigs`

**Java command** (`list-gigs` → `ListGigsCommand` / `ListGigsService`).
List every gig slug in `gigs.csv` with a song count. Quick reference before
running `copy-gig` or `export-setlist`. Reuses `SetlistAssignmentsPort` — the
same reader every other gig command uses — rather than parsing the CSV directly.
Gigs are sorted by slug (date-first slugs sort chronologically).

```zsh
./list-gigs

GIG                            SONGS
2026-05-10-rusty-nail          32
tbd                            2
```

### `find-song`

Search `.cho` files by filename fragment. Returns full paths suitable for
use in `update-song` or `updateSongsListing.txt`.

```zsh
./find-song PianoMan
# → ./cho/ABC/B/BillyJoel/PianoMan-a.cho
# → ./cho/ABC/B/BillyJoel/PianoMan.cho

./find-song HereComesMyGirl
# → ./cho/STU/T/TomPetty/HereComesMyGirl.cho
```

### `tidy-song-catalog` / `tidy-gigs`

Cleans up CSV files after a save from Google Sheets or Excel — **always run
before** `update-song`/`update-songs` or any gig command.

`tidy-song-catalog` strips Windows-style carriage returns (`\r`) from
`song-catalog.csv`.

`tidy-gigs` does more, since `gigs.csv` gets hand-edited in a spreadsheet far
more often:

- Strips `\r` from every line (same as `tidy-song-catalog`)
- **Repairs rows with missing trailing commas** — a very common spreadsheet
  artifact when the last column (`RC SLOT`) is left blank and the save drops
  the trailing comma entirely. Any row with fewer fields than the header is
  padded back out to the correct column count.
- **Refuses to guess on rows with too many fields.** These are reported with
  their line number and left completely untouched — nothing else in the file
  is written until you fix them by hand.
- Sorts all rows by `GIG` then `SET`

```zsh
./tidy-song-catalog   # cleans song-catalog.csv
./tidy-gigs           # cleans + repairs + sorts gigs.csv
```

**Sample output when a row needs manual attention:**

```
ERROR: gigs.csv has 1 row(s) with MORE fields than the header - can't safely guess how to repair these. Refusing to touch the file.
  Line 42: 2026-06-14-rusty-nail,ABC:B:BillyJoel:PianoMan,A01,5,extra-stray-value
```

### `fix-directive`

Bulk-updates all `.cho` files to replace legacy `{c:` comment directives
with the standards-compliant `{comment:` form.

```zsh
./fix-directive-dry-run   # preview
./fix-directive            # apply
```

### `lint-cho.zsh`

Lints `.cho` files for shorthand directives (e.g. `{soc}` → `{start_of_chorus}`).

```zsh
./lint-cho.zsh           # check mode — exits 1 if violations found
./lint-cho.zsh --fix     # fix mode — applies corrections in place
```

### `copyChoSetlist` / `copyAllSetlist` / `copyPdfSetlist` / `copySetlist`

Stage song files for import into OnSong or another reader app by copying
them into `./work/setlist-ff/` (project-local, gitignored — kept out of
`~/tmp` since a repo checkout under iCloud/OneDrive-synced folders can
stall on cloud placeholder files during the copy).

| Script | What it copies |
|---|---|
| `./copyChoSetlist` | All `.cho` files (recreates the target directory) |
| `./copyAllSetlist` | All `.cho` + `.pdf` files |
| `./copyPdfSetlist` | PDF lead sheets only |
| `./copySetlist` | A hand-curated gig-specific list (edit the script to match the gig) |

---

## 7. Repository Layout

```
chordprotools/
├── cho/                         # ChordPro song files (ABC/, DEF/, GHI/, ...)
│   └── <alpha-group>/<initial>/<ArtistCamelCase>/<TitleCamelCase>.cho
├── pdf/                         # PDF lead sheets and fake books
├── docs/                        # Additional documentation
│
├── song-catalog.csv             # Master song catalog (15 cols) — edit in a spreadsheet
├── gigs.csv                     # Gig assignments (GIG, SONG ID, SET, RC SLOT) — edit in a spreadsheet
│
├── build                        # Compile + package the fat JAR (run after code changes)
├── cpt                          # Internal launcher — runs the JAR directly (java -jar), auto-builds if missing
│
├── import-song                  # Register a new .cho file in the catalog
├── verify-catalog               # Check catalog ↔ .cho file consistency
├── consistent-metadata          # Check key-variants share consistent metadata (--fix to repair)
├── transpose                    # Create a new key-variant of a song by SONG ID (transposes + auto-catalogs it)
├── verify-sync                  # Compare any two .cho files for semantic (lyric/harmonic) drift
├── consistent-song-data         # Check key-variants of a song share the same lyrics/harmony, not just metadata
├── update-song                  # Push one song's catalog metadata (by song ID) to its .cho file(s)
├── update-songs                 # Push a batch of songs (by song ID) from updateSongsListing.txt
├── assign-backing-track-slots   # Assign RC-500 slot numbers for the gig; writes to gigs.csv + patches .cho files + regenerates setlist.csv
├── copy-gig                     # Clone a gig's assignments to a new gig slug
├── export-setlist               # Generate setlist.csv from catalog + assignments
├── deploy-rc500                 # Generates deploy-rc500-<timestamp>.sh with cp commands for RC-500 audio files
│
├── find-song-id                 # Search catalog by title/artist → SONG ID
├── list-gigs                    # List all gig slugs with song counts
├── find-song                    # Search .cho filenames by fragment → file path
│
├── tidy-song-catalog            # Strip Windows \r from song-catalog.csv
├── tidy-gigs                    # Strip Windows \r from gigs.csv
├── lint-cho.zsh                 # Lint/fix shorthand ChordPro directives in .cho files
├── fix-directive                # Bulk-replace {c: with {comment: in .cho files
├── fix-directive-dry-run        # Preview fix-directive changes
├── copyChoSetlist               # Stage all .cho files for OnSong import
├── copyAllSetlist               # Stage all .cho + .pdf files
├── copyPdfSetlist               # Stage PDF lead sheets
├── copySetlist                  # Stage a hand-curated gig setlist
├── help                         # Show CLI help
│
└── src/
    └── main/java/com/pourchoices/chordpro/
        ├── adapter/in/file/          # picocli commands (CLI entry points)
        ├── adapter/out/file/         # CSV readers, writers, mappers
        ├── application/domain/
        │   ├── model/                # Domain objects: CatalogEntry, SetlistAssignment, ...
        │   └── service/              # Business logic: SetlistJoiner, SetlistDeduplicator, ...
        ├── application/port/in/      # Use case interfaces (input ports)
        └── application/port/out/     # Repository interfaces (output ports)
```

---

## 8. Technology Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5 |
| CLI | [picocli](https://picocli.info) via `picocli-spring-boot-starter` |
| CSV I/O | [OpenCSV](https://opencsv.sourceforge.net) |
| Build | Apache Maven |

The application follows a [Hexagonal Architecture](https://en.wikipedia.org/wiki/Hexagonal_architecture_(software))
pattern — business logic in the domain layer has no dependency on how data
is stored or how commands are invoked.

---

## 9. How to Add a New Command

Adding a command requires touching files in a consistent, layered order.
`CopyGigService` and `ExportSetlistService` are good reference implementations.

### Step 1 — Domain model (if needed)

Create an immutable domain object in `application/domain/model/` using
Lombok `@Value` + `@Builder`, matching the pattern of `CatalogEntry` and
`SetlistAssignment`.

### Step 2 — Input port (use case interface)

Define what the command *does* as an interface in `application/port/in/`:

```java
public interface MyNewFeatureUseCase {
    int doTheThing(String input);
}
```

### Step 3 — Service implementation

Write business logic in `application/domain/service/`:

```java
@Service
@Slf4j
public class MyNewFeatureService implements MyNewFeatureUseCase {

    private final CatalogPort catalogPort;
    private final ChordproCatalogIndexPathConfig config;

    public MyNewFeatureService(CatalogPort catalogPort,
                               ChordproCatalogIndexPathConfig config) {
        this.catalogPort = catalogPort;
        this.config      = config;
    }

    @Override
    public int doTheThing(String input) {
        // business logic — depends only on ports and domain objects
    }
}
```

### Step 4 — picocli command

Add the CLI adapter in `adapter/in/file/`:

```java
@Component
@Command(name = "my-new-command", description = "Does the thing.")
@Slf4j
public class MyNewCommand implements Runnable {

    private final MyNewFeatureUseCase useCase;

    public MyNewCommand(MyNewFeatureUseCase useCase) {
        this.useCase = useCase;
    }

    @Parameters(index = "0", description = "The input value.")
    private String input;

    @Override
    public void run() {
        int result = useCase.doTheThing(input);
        System.out.printf("Done: %d item(s) processed.%n", result);
    }
}
```

Use `@Parameters` for positional arguments and `@Option` for named flags.

### Step 5 — Register the command

Add the new class to the `subcommands` list in `ChordproToolsMainCommand`:

```java
subcommands = {
    ImportNewSongCommand.class,
    VerifyCatalogCommand.class,
    UpdateSongCommand.class,
    UpdateSongsCommand.class,
    AssignBackingTrackSlotsCommand.class,
    CopyGigCommand.class,
    ExportSetlistCommand.class,
    MyNewCommand.class        // ← add here
}
```

### Step 6 — Shell script (optional but recommended)

```zsh
#!/bin/zsh
mvn -q spring-boot:run \
  -Dspring-boot.run.arguments="my-new-command $*"
```

```zsh
chmod +x my-new-command-script
```

### Step 7 — Test

Add a unit test under `src/test/`, mirroring the existing structure.
Mock collaborators with Mockito; test the service logic independently
of the Spring context. See `CopyGigServiceTest` for a complete example.

### Step 8 — Update this README

Add a row to the [Command Summary](#4-command-summary) table and a
section under [Commands](#5-commands).

---

## License

MIT License — see `LICENSE` for details.
