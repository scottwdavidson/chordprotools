# Pour Choices CLI — ChordPro Catalog Manager

A command-line tool built for the [Pour Choices band](https://pourchoicesmusic.com) to manage the
band's full library of ChordPro (`.cho`) song files.

Two CSV files are the heart of the system:

| File | Role |
|---|---|
| `song-catalog.csv` | Master song library — one row per `.cho` file, all metadata |
| `setlist-assignments.csv` | Gig assignments — which songs are in which gig and in what order |

They are kept deliberately separate so the catalog can be a stable, long-lived reference while setlists
change freely gig to gig without touching song metadata.

---

## Table of Contents

1. [Overview](#1-overview)
2. [Data Model](#2-data-model)
   - [song-catalog.csv](#song-catalogcsv)
   - [setlist-assignments.csv](#setlist-assignmentscsv)
   - [Song Versions and Key Variants](#song-versions-and-key-variants)
3. [Workflows](#3-workflows)
4. [Command Summary](#4-command-summary)
5. [Commands](#5-commands)
   - [generate-song-catalog](#generate-song-catalog)
   - [update-song / update-songs](#update-song--update-songs)
   - [assign-backing-track-slots](#assign-backing-track-slots)
   - [copy-gig](#copy-gig)
   - [export-setlist](#export-setlist)
   - [import-new-song](#import-new-song)
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
                               │  generate-song-catalog
                               ▼
                       song-catalog.csv          ◄── edit in Sheets/Excel
                               │
                               │  update-song / update-songs
                               ▼
                       .cho files updated
                               │
                               │  assign-backing-track-slots
                               ▼
                SONG LABEL written to .cho + catalog


┌─────────────────────────────────────────────────────────────────────┐
│                     setlist-assignments.csv                          │
│         (edit in Sheets — assign songs to gigs and set positions)   │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
           copy-gig            │  export-setlist
      (clone prior gig)        ▼
                       setlist.csv  +  terminal table
```

The catalog and the setlist are **independent edit surfaces**. You can
re-key a song, rename an artist, or update a Nord preset in the catalog
without touching any setlist. Likewise you can rebuild a setlist from
scratch without disturbing the catalog.

---

## 2. Data Model

### `song-catalog.csv`

One row per `.cho` file. All metadata for that specific file version lives here.

| Field | Description |
|---|---|
| `TITLE` | Song title |
| `ARTIST` | Artist or band name |
| `KEY` | Musical key of this specific `.cho` file |
| `DURATION` | Song duration (e.g. `3:30`) |
| `TEMPO` | BPM |
| `COUNTIN` | Count-in type — beat buddy, backing track, etc. |
| `BACKING` | RC-500 loop station slot number for the backing track |
| `NORD` | Voice preset on the Nord keyboard |
| `ROLAND` | Voice preset on the Roland keyboard |
| `VE` | Vocal effects preset |
| `PERFORMANCE KEY` | Key the band actually performs in (may differ from the file's `KEY`) |
| `TIME SIGNATURE` | Time signature (e.g. `4/4`) |
| `CAPO` | Guitar capo position, if any |
| `SONG ID` | Unique identifier — derived from the file path (e.g. `ABC:B:BillyJoel:PianoMan`) |
| `SONG LABEL` | RC-500 display label — shown on the guitarist's loop station screen |

> **`CHORDPRO FILENAME` is not stored in the CSV.** The `SONG ID` encodes the
> full path implicitly. The file `cho/ABC/B/BillyJoel/PianoMan.cho` maps to
> `SONG ID = ABC:B:BillyJoel:PianoMan`.

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

### `setlist-assignments.csv`

One row per song-in-gig assignment. Songs appear here only when they are
assigned to a specific gig.

| Field | Description |
|---|---|
| `GIG` | Gig identifier slug, date-first (e.g. `2026-06-14-rusty-nail`) |
| `SONG ID` | Foreign key into `song-catalog.csv` — **always the base version** (no key suffix) |
| `SET` | Compound position code — encodes set letter and song position within it |
| `TITLE` | Decorative — populated automatically for readability in Sheets; not used by code |
| `ARTIST` | Decorative — same as above |

#### SET code convention

| Code | Meaning |
|---|---|
| `A01` | Set A, song 1 |
| `A02` | Set A, song 2 |
| `B01` | Set B, song 1 |
| `C03` | Set C, song 3 |

Songs are sorted by SET code when a setlist is exported.

#### Foreign key rule

`setlist-assignments.csv` references songs by **base SONG ID only** — never
a key-variant ID. The system enforces this at read time and will throw a
descriptive error if a variant ID (e.g. `PianoMan-a`) is found in the file,
telling you exactly what to change it to.

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

- `BACKING` and `SONG LABEL` metadata are written to **all** versions of a
  song, because the guitarist may open either `.cho` file on stage and needs
  to see the RC-500 slot number regardless of which one is on screen.
- Setlists reference only the base SONG ID — the system treats "Piano Man"
  as one song, not two, regardless of how many transposed files exist.

Use `./find-song-id` to discover SONG IDs for use in setlist assignments:

```zsh
./find-song-id "piano"
```

```
TITLE                   ARTIST       KEY   SONG ID                         VARIANTS
Piano Man               Billy Joel   C     ABC:B:BillyJoel:PianoMan        +1 key variant
```

The `SONG ID` column always shows the base version — safe to paste directly
into `setlist-assignments.csv`.

---

## 3. Workflows

### Bootstrapping or refreshing the catalog

```zsh
./generate-song-catalog       # scan all .cho files → song-catalog.csv
# open song-catalog.csv in Google Sheets, fill in metadata
./tidy-song-catalog           # strip Windows \r artifacts after saving from Sheets
./update-songs                # push metadata from catalog back to .cho files
./assign-backing-track-slots  # reassign RC-500 slot numbers, update catalog + .cho files, write setlist.csv
```

### Planning a new gig

```zsh
./list-gigs                                                   # see existing gigs + song counts
./copy-gig 2026-05-10-rusty-nail 2026-06-14-rusty-nail        # clone a prior gig as a starting point
# open setlist-assignments.csv in Sheets
# reorder SET codes, swap songs as needed (TITLE/ARTIST columns help identify songs)
./export-setlist --gig 2026-06-14-rusty-nail                  # generate setlist.csv + preview
```

### Adding songs to an existing gig

```zsh
./find-song-id "joel"              # find the SONG ID for the song you want
# paste the SONG ID + gig + SET code into setlist-assignments.csv in Sheets
./export-setlist --gig <gig-slug>  # verify the setlist looks right
```

### Updating song metadata

```zsh
# edit the row in song-catalog.csv in Sheets, then:
./tidy-song-catalog
./find-song PianoMan               # get the .cho file path
# update ./update-song with the path, then:
./update-song
```

---

## 4. Command Summary

| Script | CLI Subcommand | Description |
|---|---|---|
| `./generate-song-catalog` | `generate-song-catalog` | Scan all `.cho` files and rebuild `song-catalog.csv` from scratch |
| `./update-song` | `update-song` | Push catalog metadata into one specific `.cho` file |
| `./update-songs` | `update-songs` | Push catalog metadata into a batch of `.cho` files |
| `./assign-backing-track-slots` | `assign-backing-track-slots` | Reassign RC-500 backing-track slot numbers for the gig; updates `song-catalog.csv`, affected `.cho` files, and writes `setlist.csv` |
| `./copy-gig` | `copy-gig` | Clone all setlist assignments from one gig to a new gig slug |
| `./export-setlist` | `export-setlist` | Join catalog + assignments and export a gig-ready `setlist.csv` |
| *(direct only)* | `import-new-song` | *(Not yet implemented)* Add a single new `.cho` to the catalog |

Quick help at any time:

```zsh
./help
```

---

## 5. Commands

### `generate-song-catalog`

**Script:** `./generate-song-catalog`

Recursively scans every `.cho` file under `cho/`, parses ChordPro headers,
and writes a fresh `song-catalog.csv`. Use this when bootstrapping or after
adding a batch of new songs.

> ⚠️ This **overwrites** the existing `song-catalog.csv`. Ensure any
> unsaved spreadsheet edits have been applied via `update-song` / `update-songs` first.

```zsh
./generate-song-catalog
```

After running: open `song-catalog.csv` in Google Sheets, fill in any new
metadata rows, then run `./tidy-song-catalog` before applying changes back.

---

### `update-song` / `update-songs`

**Scripts:** `./update-song`, `./update-songs`

Reads metadata from `song-catalog.csv` and writes it back into `.cho` file
headers. This is the primary way catalog edits flow back into the song files.

**Single song:**

```zsh
# Edit the path inside ./update-song to point at the target file, then:
./update-song

# Find a song's path first:
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

The command to run when finalising a gig's setlist order. It reassigns RC-500
backing-track slot numbers for every set-assigned song that has a real backing
track, then propagates those numbers into the catalog and the individual `.cho`
files so the guitarist's loop station reflects the running order.

**What it does, in order:**

1. Loads `song-catalog.csv` + `setlist-assignments.csv` and joins them for the target gig
2. Splits songs into **in-set** (SET prefix A–Y, sorted by SET code) and **backup** (SET prefix Z, sorted alphabetically by title)
3. Assigns RC-500 slot numbers sequentially — in-set from slot **5** upward, backup from slot **50** upward
4. Writes the updated `song-catalog.csv` with the new `BACKING` values
5. Calls `update-song` for every `.cho` file whose slot number changed
6. Writes a fresh `setlist.csv`

```zsh
./assign-backing-track-slots                              # latest gig
./assign-backing-track-slots --gig 2026-06-14-rusty-nail  # specific gig
```

> Songs with no `BACKING` value, or the sentinel value `99`, are included in the
> setlist but skipped during slot assignment — they have no backing track to cue.

> Slots 1–4 are intentionally left free. Slots 50–99 are reserved for backup songs.

---

### `copy-gig`

**Script:** `./copy-gig <source-gig> <target-gig> [--force]`

Clones all setlist assignments from an existing gig to a new gig slug. The
target's `setlist-assignments.csv` rows are written with `TITLE` and `ARTIST`
populated from the catalog so the CSV is immediately readable in Google Sheets
without cross-referencing `song-catalog.csv`.

```zsh
# Start next month's gig from last month's setlist
./copy-gig 2026-05-10-rusty-nail 2026-06-14-rusty-nail

# Re-clone over a target you have already started editing
./copy-gig 2026-05-10-rusty-nail 2026-06-14-rusty-nail --force
```

Guard-rails:
- Source gig must exist in `setlist-assignments.csv` — throws if not found
- Target gig must not already have assignments unless `--force` is passed
- Base-version enforcement is inherited: the cloned rows use base SONG IDs only

After cloning, open `setlist-assignments.csv` in Google Sheets, adjust SET
codes to reorder songs, and swap in different SONG IDs where the set list
differs from the prior gig.

---

### `export-setlist`

**Script:** `./export-setlist`

Joins `song-catalog.csv` and `setlist-assignments.csv` for a specific gig,
sorts by SET code, and writes a gig-ready `setlist.csv`. Also prints a
formatted summary table to the terminal.

```zsh
# Export the most recent gig (lexicographically last GIG slug)
./export-setlist

# Export a specific gig
./export-setlist --gig 2026-06-14-rusty-nail

# Custom output path
./export-setlist --gig 2026-06-14-rusty-nail --output ./gig-2026-06-14.csv
```

**Terminal output example:**

```
Setlist export complete — 8 songs written to ./setlist.csv

SET     TITLE                          ARTIST              KEY    BACKING
--------------------------------------------------------------------------------
A01     How Long                       Ace                 A      12
A02     Year of the Cat                Al Stewart          Em
A03     Piano Man                      Billy Joel          C      7
A04     Daniel                         Elton John          C      3
B01     Against the Wind               Bob Seger           G
B02     Landslide                      Fleetwood Mac       C      9
B03     My Life                        Billy Joel          D      5
B04     Ebony Eyes                     Bob Welch           Gm     2
```

> `export-setlist` will throw if any assigned SONG ID is not found in
> `song-catalog.csv`. A missing base version is treated as a data integrity
> error, not a silent skip.

---

### `import-new-song`

> ⚠️ **Not yet implemented.** Invoking this command throws
> `UnsupportedOperationException`. The wiring (command class, port interface)
> is in place as a drop-in socket for a future implementation.
>
> For now, add new songs by running `./generate-song-catalog` to regenerate
> the full catalog from all `.cho` files.

---

## 6. Utility Scripts

These scripts are pure shell or Python — they do not invoke the Java application
unless noted.

### `find-song-id`

Search `song-catalog.csv` by title or artist fragment. Prints one row per song
(base version only), annotating how many key variants exist. The `SONG ID`
column is always a valid base ID safe to paste into `setlist-assignments.csv`.

```zsh
./find-song-id "piano"

TITLE         ARTIST       KEY   SONG ID                   VARIANTS
Piano Man     Billy Joel   C     ABC:B:BillyJoel:PianoMan  +1 key variant

./find-song-id "joel"
# → all Billy Joel songs
```

### `list-gigs`

List every gig slug in `setlist-assignments.csv` with a song count. Quick
reference before running `copy-gig` or `export-setlist`.

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

### `tidy-song-catalog`

Strips Windows-style carriage returns (`\r`) from `song-catalog.csv`.
Always run this after saving the catalog from Google Sheets or Excel before
running any update command.

```zsh
./tidy-song-catalog
```

### `fix-directive`

Bulk-updates all `.cho` files to replace legacy `{c:` comment directives
with the standards-compliant `{comment:` form.

```zsh
./fix-directive-dry-run   # preview
./fix-directive            # apply
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
├── song-catalog.csv             # Master song catalog — edit in a spreadsheet
├── setlist-assignments.csv      # Gig assignments — edit in a spreadsheet
│
├── generate-song-catalog        # Rebuild catalog from all .cho files
├── update-song                  # Push one song's catalog metadata to its .cho
├── update-songs                 # Push a batch of songs from updateSongsListing.txt
├── assign-backing-track-slots   # Reassign RC-500 slot numbers for the gig; update catalog + .cho files + setlist.csv
├── copy-gig                     # Clone a gig's assignments to a new gig slug
├── export-setlist               # Generate setlist.csv from catalog + assignments
│
├── find-song-id                 # Search catalog by title/artist → SONG ID
├── list-gigs                    # List all gig slugs with song counts
├── find-song                    # Search .cho filenames by fragment → file path
│
├── tidy-song-catalog            # Strip Windows \r from catalog CSV
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
    GenerateSongCatalogCommand.class,
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
mvn spring-boot:run \
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
