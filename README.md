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
   - [update-song / update-songs](#update-song--update-songs)
   - [assign-backing-track-slots](#assign-backing-track-slots)
   - [copy-gig](#copy-gig)
   - [export-setlist](#export-setlist)
6. [Utility Scripts](#6-utility-scripts)
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
| `CAPO` | Guitar capo position, if any |
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

### `gigs.csv`

One row per song-in-gig assignment. Songs appear here only when they are
assigned to a specific gig.

| Field | Description |
|---|---|
| `GIG` | Gig identifier slug, date-first (e.g. `2026-06-14-rusty-nail`) |
| `SONG ID` | Foreign key into `song-catalog.csv` — **always the base version** (no key suffix) |
| `SET` | Compound position code — encodes set letter and song position within it |
| `RC SLOT` | RC-500 slot assigned for this gig — blank until `assign-backing-track-slots` is run |

> **RC SLOT is gig-specific.** Running `assign-backing-track-slots` for one gig
> populates that gig's rows only — other gigs are never touched. `copy-gig` always
> copies rows with a blank RC SLOT so the new gig starts fresh and gets its own
> independent slot assignment.

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
- `{meta: rc-slot: N}` is written by `assign-backing-track-slots` to all
  `.cho` versions of a song, so the slot is visible regardless of which file
  is open on the iPad.
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
#    The file must have at minimum:
#      {title: Movin' Out}    ← human-readable; must match the path identity
#      {artist: Billy Joel}   ← same
#      {key: A}               ← strongly recommended; fill rest in Sheets later
#
# 2. Import it — SONG ID is derived from the path automatically:
./import-song cho/ABC/B/BillyJoel/MovingOut.cho --dry-run   # preview first
./import-song cho/ABC/B/BillyJoel/MovingOut.cho             # add to catalog
#
# 3. Open song-catalog.csv in Google Sheets, fill in remaining metadata
# 4. Save CSV → tidy → push metadata back to the file:
./tidy-song-catalog
./update-song cho/ABC/B/BillyJoel/MovingOut.cho
#
# 5. Confirm catalog and file agree:
./verify-catalog
```

### Updating song metadata

```zsh
# Edit the row in song-catalog.csv in Sheets, then:
./tidy-song-catalog
./update-song cho/ABC/B/BillyJoel/PianoMan.cho   # or use ./find-song PianoMan to get the path
./verify-catalog
```

### Planning a new gig

```zsh
./list-gigs                                                   # see existing gigs + song counts
./copy-gig 2026-05-10-rusty-nail 2026-06-14-rusty-nail        # clone a prior gig as a starting point
# open gigs.csv in Sheets
# reorder SET codes, swap songs as needed
./export-setlist --gig 2026-06-14-rusty-nail                  # preview the fan setlist (no Z-sets)
./export-setlist --gig 2026-06-14-rusty-nail --verbose        # preview with backup songs too
```

### Locking in a gig (finalising backing-track slots)

Run `assign-backing-track-slots` **after** the setlist order is finalised —
slot numbers are derived from SET code order, so changes to order after assignment
require a re-run.

```zsh
./assign-backing-track-slots --gig 2026-06-14-rusty-nail
# → Slots written to gigs.csv (this gig only)
# → {meta: rc-slot: N} patched into each affected .cho file
# → setlist.csv regenerated
```

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
| `./update-song` | `update-song` | Push catalog metadata into one specific `.cho` file |
| `./update-songs` | `update-songs` | Push catalog metadata into a batch of `.cho` files |
| `./assign-backing-track-slots` | `assign-backing-track-slots` | Assign RC-500 slot numbers for the gig; writes to `gigs.csv` + patches `.cho` files; regenerates `setlist.csv` |
| `./copy-gig` | `copy-gig` | Clone all gig assignments from one gig slug to a new one |
| `./export-setlist` | `export-setlist` | Join catalog + assignments and export a gig-ready `setlist.csv` |

Quick help at any time:

```zsh
./help
```

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

The SONG ID is derived from the **file path**, so technically no directives
are required for the import to succeed. In practice, the file should have at
minimum:

| Directive | Status | Why |
|---|---|---|
| `{title: ...}` | **Required** | Catalog display, setlist output, `find-song-id` search |
| `{artist: ...}` | **Required** | Same — a catalog row without an artist is a ghost |
| `{key: ...}` | Strongly recommended | `export-setlist` KEY column; you know this at chart time |
| Everything else | Fill in later in Sheets | Hardware presets, tempo, duration, label, etc. |

The path is the **canonical identity** — the directives are its human-readable
form. `BillyJoel/MovingOut.cho` should have `{artist: Billy Joel}` and
`{title: Movin' Out}`: same entity, different representation. They don't need
to be character-for-character identical, but they must be unambiguously the
same song.

#### Minimum viable `.cho` file

```
{title: Movin' Out}
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

### `update-song` / `update-songs`

**Scripts:** `./update-song <path>`, `./update-songs`

Reads metadata from `song-catalog.csv` and writes it back into `.cho` file
headers. This is the primary way catalog edits flow back into the song files.

> `update-song` preserves any `{meta: rc-slot: N}` already in the file —
> a slot assigned by `assign-backing-track-slots` is never erased by a
> catalog update.

**Single song:**

```zsh
./update-song cho/ABC/B/BillyJoel/PianoMan.cho

# Find a song's path first if needed:
./find-song PianoMan
# → ./cho/ABC/B/BillyJoel/PianoMan.cho
```

**Batch:**

```zsh
# Edit updateSongsListing.txt — one .cho path per line
./update-songs
```

---

### `assign-backing-track-slots`

**Script:** `./assign-backing-track-slots [--gig <slug>] [--output <path>]`

The command to run when finalising a gig's setlist order. It assigns RC-500
backing-track slot numbers for every set-assigned RC song, then propagates
those numbers into `gigs.csv` and directly into the individual `.cho` files.

**What it does, in order:**

1. Loads `song-catalog.csv` + `gigs.csv` and resolves the target gig
2. Splits songs into **in-set** (SET prefix A–Y, sorted by SET code) and **backup** (SET prefix Z, sorted alphabetically by title)
3. Assigns RC-500 slot numbers — in-set from slot **5** upward, backup from slot **50** upward
4. Writes RC SLOT values for this gig into `gigs.csv` (**only this gig's rows are updated**)
5. Patches `{meta: rc-slot: N}` directly into each affected `.cho` file
6. Writes a fresh `setlist.csv`

```zsh
./assign-backing-track-slots                              # latest gig
./assign-backing-track-slots --gig 2026-06-14-rusty-nail  # specific gig
```

> **BeatBuddy songs** (`BACKING=BB`) are included in the setlist but skipped
> during slot assignment — BeatBuddy beat selection is done on the pedal itself.

> **Songs with no backing** (`BACKING` blank) are likewise skipped.

> Slots 1–4 are intentionally left free. Slots 50–99 are reserved for backup songs.

> Assignments for **other gigs are never touched**. Each gig maintains its own
> independent set of slot numbers.

---

### `copy-gig`

**Script:** `./copy-gig <source-gig> <target-gig> [--force]`

Clones all setlist assignments from an existing gig to a new gig slug. The
new rows are written with `TITLE` and `ARTIST` enriched from the catalog so
the CSV is immediately readable in Google Sheets.

```zsh
# Start next month's gig from last month's setlist
./copy-gig 2026-05-10-rusty-nail 2026-06-14-rusty-nail

# Re-clone over a target you have already started editing
./copy-gig 2026-05-10-rusty-nail 2026-06-14-rusty-nail --force
```

**RC SLOT is never copied.** The new gig's rows always start with a blank RC SLOT
column so slot numbers are assigned fresh when `assign-backing-track-slots` is run
for the new gig.

Guard-rails:
- Source gig must exist in `gigs.csv` — throws if not found
- Target gig must not already have assignments unless `--force` is passed

After cloning, open `gigs.csv` in Google Sheets, adjust SET codes to reorder
songs, and swap in different SONG IDs where the set list differs from the prior gig.

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

These scripts are pure shell or Python — they do not invoke the Java application
unless noted.

### `find-song-id`

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

### `list-gigs`

List every gig slug in `gigs.csv` with a song count. Quick reference before
running `copy-gig` or `export-setlist`.

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

Strips Windows-style carriage returns (`\r`) from `song-catalog.csv` and `gigs.csv`
respectively. Always run after saving either file from Google Sheets or Excel before
running any update command.

```zsh
./tidy-song-catalog   # cleans song-catalog.csv
./tidy-gigs           # cleans gigs.csv
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
them into `~/tmp/setlist-ff/`.

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
├── import-song                  # Register a new .cho file in the catalog
├── verify-catalog               # Check catalog ↔ .cho file consistency
├── update-song                  # Push one song's catalog metadata to its .cho file
├── update-songs                 # Push a batch of songs from updateSongsListing.txt
├── assign-backing-track-slots   # Assign RC-500 slot numbers for the gig; update gigs.csv + .cho files + setlist.csv
├── copy-gig                     # Clone a gig's assignments to a new gig slug
├── export-setlist               # Generate setlist.csv from catalog + assignments
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
