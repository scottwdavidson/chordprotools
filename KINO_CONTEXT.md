# KINO_CONTEXT — chordprotools
# Agent-owned. Updated end-of-session. Not for human consumption.
# Last updated: 2026-05-17 (session 10)

---

## WHAT THIS IS

CLI tool for the **Pour Choices band** (pourchoicesmusic.com) to manage ~500 ChordPro (`.cho`) song files and the metadata that drives live gigs. Extends standard ChordPro headers with band-specific fields for hardware presets (Nord piano, Roland keyboard, VE-500 vocal effects, RC-500 looper backing tracks, BeatBuddy count-ins) and set management.

**Single source of truth:** `song-catalog.csv` (root of repo)
**Song files:** `cho/**/*.cho` — consumed by OnSong on stage

---

## TECH STACK

| Item | Value |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5.0 |
| CLI | picocli 4.7.7 via picocli-spring-boot-starter |
| CSV | OpenCSV 5.11.2 |
| Build | Apache Maven (mvnw wrapper present) |
| Tests | JUnit + Mockito 5.18 |
| Lombok | yes — @Value, @Builder, @AllArgsConstructor on everything |
| Architecture | Hexagonal (ports & adapters) — domain never imports adapters |

Run via: `mvn spring-boot:run -Dspring-boot.run.arguments="<subcommand> <args>"`

---

## SONG CATALOG CSV COLUMNS (in order)

```
TITLE, ARTIST, KEY, DURATION, TEMPO, COUNTIN, BACKING, NORD, ROLAND, VE,
PERFORMANCE KEY, TIME SIGNATURE, CAPO, SONG ID, SONG LABEL
```

(15 columns. SET was removed in Phase 3 — it now lives exclusively in `setlist-assignments.csv`.)

- **SONG ID**: structured key `CLUSTER:LETTER:ArtistDir:SongStem[-keyVariant]`
  - e.g. `ABC:B:BillyJoel:MyLife` or `ABC:B:BillyJoel:MyLife-c`
  - Maps 1:1 to filesystem path under `./cho/`
- **SONG LABEL**: RC-500 display label for click/backing tracks — **max 12 chars** (hardware limit). Placed between SONG ID and SET. Written to .cho as `{meta: label: ...}`. Logs WARN on read if > 12 chars.
- **SET**: sortable alphanumeric code e.g. `A01`, `A02`, `B01` — drives `export-setlist`
- **PERFORMANCE KEY**: key band actually plays in (may differ from chart key in `.cho` file)
- **COUNTIN**: count-in source (e.g. `4` = BeatBuddy, `8` = backing track, `24` = default)
- **BACKING**: track number on RC-500 looper (e.g. `48`)
- **NORD**: Nord piano voice preset (e.g. `M11`, `M22`)
- **ROLAND**: Roland keyboard voice preset
- **VE**: VE-500 vocal harmony preset (e.g. `U99`)

Mandatory fields (always present in `.cho` header): TITLE, ARTIST, KEY, DURATION, TEMPO
Optional fields: only written to `.cho` when non-blank in catalog

---

## DOMAIN MODEL KEY CLASSES

### SongId (immutable @Value)
Structured identity: `clusterPrefix:clusterElement:artist:title[-keyAlternative]`
- `isBaseVersion()` → true when no keyAlternative (e.g. `MyLife` not `MyLife-c`)
- `toGroupKey()` → strips keyAlternative for dedup grouping
- KEY_ALT_PATTERN: `-[a-gA-G][#b]?m?` — matches `-c`, `-am`, `-g#m`, `-bb` but NOT `-old`, `-MVP`, `-orig`
- `ChordProPath.toSongId(path)` converts file path ↔ SongId

### CatalogEntry (@Value @Builder)
Flat domain object, one per catalog row. Has all fields above.

### HeaderDirective (enum)
All recognized `.cho` header directives with:
- `prefixes`: list of strings that parse to this directive
- `cardinality`: ordering (TITLE=90 highest, UNPARSED_META=1 lowest)
- `mandatory`: whether always written
- `meta`: whether written as `{meta: directive: value}` vs `{directive: value}`
- Key directives: TITLE(90), ARTIST(89), KEY(88), DURATION(87), TEMPO(86), COUNTIN(42), BACKING(30), SONG_LABEL(29), VE(28), PERFORMANCE_KEY(20), NORD(50), ROLAND(49)

### Rc500MemoryBank / Rc500Slot / Rc500Track (@Value @Builder)
Domain model for the RC-500 `.RC0` memory-bank file format.
- `Rc500MemoryBank`: holds `List<Rc500Slot>` + `findByIndex(int)` helper
- `Rc500Slot`: one slot — `slotIndex`, `name`, `backingTrack: Rc500Track`, `clickTrack: Rc500Track`, `hasAnyAudio()`
- `Rc500Track`: audio track presence / metadata within a slot
- Port: `Rc500Port` (out) — `readMemoryBank(path)` / `writeMemoryBank(path, bank)` (read-modify-write strategy)
- Adapter stack: `Rc500Adapter`, `Rc500FileReader`, `Rc500FileWriter`, `Rc500Mapper`, `Rc500SlotDto`, `Rc500TrackDto`, `Rc500AssignDto`, `Rc500ParseException` — all in `adapter/out/file/`
- **Not yet wired to any picocli command** — infrastructure is in place; command TBD

### ChordProTransposer (static utility class)
Transposes ChordPro-formatted lines by ±N half-steps. Handles sharps/flats, enharmonic equivalents, double accidentals.
Key methods: `transpose(line, halfSteps, useFlats)`, `transposeUp(line, halfSteps)`, `transposeDown(line, halfSteps)`.
Not wired to any command yet — available for future transposition use case.

### SongDirective (domain model)
Represents a single parsed directive line from a `.cho` file body (non-header). Exists in `application/domain/model/`. Details TBD.

### ParsedSong
A parsed `.cho` file: ParsedHeader + raw body lines (chords/lyrics unchanged)

### SetlistEntry (@Value @Builder)
Join result: one `CatalogEntry` (song metadata) + one `SetlistAssignment` (gig position).
Delegate methods: `getSet()`, `getGig()`, `getSongId()`, `getTitle()`, `getArtist()`, `getKey()`, `getBacking()`, `getPerformanceKey()`.
This is the primary unit of currency for all setlist-producing services.

### SetlistJoiner (@Component)
Resolves gig slug (explicit `--gig` param or falls back to lexicographically last GIG value in assignments),
filters `SetlistAssignment` rows to that gig, joins with the catalog map, and returns `List<SetlistEntry>`.
Shared by `ExportSetlistService` and `AssignBackingTrackSlotsService`.
Key methods: `join(gigParam, allAssignments, catalog)`, `resolveGig(gigParam, allAssignments)`.

### Setlist (@Value @Builder)
`gig: String` + `entries: List<SetlistEntry>` + `size()`. Produced by both setlist services, consumed by CLI commands for stdout rendering and summary output.

---

## COMMANDS

### `generate-song-catalog` → `GenerateSongCatalogCommand/Service`
- Reads songsListing.txt (one .cho path per line), parses every file, writes full catalog
- **OVERWRITES** entire catalog — clears SET/PERFORMANCE KEY etc. that aren't in .cho headers
- Script: `./generate-song-catalog` (does find+sort into songsListing.txt first)
- Flow: listing file → parse each .cho → CatalogEntry → write CSV

### `update-song` → `UpdateSongCommand/Service`
- Catalog → one .cho file: reads catalog, finds entry by SongId, maps to ParsedHeader, compares to current file, writes only if changed
- Body (chords/lyrics) is preserved entirely
- Script: `./update-song` (edit target path inside script before running)
- Catalog key: SongId string derived from file path via `ChordProPath.toSongId()`

### `update-songs` → `UpdateSongsCommand/Service`
- Batch version of update-song: reads updateSongsListing.txt, calls UpdateSongService per file
- Script: `./update-songs`
- Edit `updateSongsListing.txt` with one .cho path per line

### `assign-backing-track-slots` → `AssignBackingTrackSlotsCommand/Service`
- Loads `song-catalog.csv` + `setlist-assignments.csv`, resolves target gig via `SetlistJoiner`, deduplicates via `SetlistDeduplicator`, splits into in-set (A–Y prefix) vs backup (Z prefix)
- Reassigns RC-500 slot numbers for all gig-assigned songs that have a real backing track
- **In-set songs** (SET prefix A–Y): sorted by SET code, slots assigned from **5** upward
- **Backup songs** (SET prefix Z, e.g. Z0, Z1): sorted alphabetically by title, slots from **50** upward (hard cap at 99)
- Songs with BACKING blank or "99" are skipped (no slot change)
- Side-effects in order: write updated catalog CSV → call UpdateSongService per changed .cho → re-join + write fresh setlist.csv
- Options: `--gig`/`-g` (slug; auto-resolves to latest gig if omitted), `--output`/`-o` (default `./setlist.csv`)
- Constants in service: `IN_SET_START_SLOT=5`, `BACKUP_START_SLOT=50`, `MAX_SLOT=99`
- Dedup reuses `SetlistDeduplicator` component

### `export-setlist` → `ExportSetlistCommand/Service`
- Loads `song-catalog.csv` + `setlist-assignments.csv`, resolves target gig, joins via `SetlistJoiner`, deduplicates via `SetlistDeduplicator`, sorts by SET code, writes `setlist.csv`
- Prints formatted table to stdout (SET / TITLE / ARTIST / KEY / BACKING)
- Options: `--gig`/`-g` (slug e.g. `2026-06-14-rusty-nail`; auto-resolves to lexicographically latest gig if omitted), `--output`/`-o` (default `./setlist.csv`)
- **Dedup logic**: extracted from `ExportSetlistService` into `SetlistDeduplicator` (@Component) — shared by `ExportSetlistService` and `AssignBackingTrackSlotsService`
- **SetlistEntryDto**: includes `backing` column (blank when no backing or sentinel 99). Order: set, song title, song artist, key, backing
- **ExportSetlistCommand stdout**: shows SET / TITLE / ARTIST / KEY / BACKING
- **Dedup rules** (base=no keyAlt, variant=has keyAlt):
  - Scenario A (base+variant, same SET): keep base, drop variant [INFO]
  - Scenario B (only variant has SET): single-member group, keep
  - Scenario C (base+variant, different SET): keep base, warn [WARN]
  - Both variants same SET, no base: keep first [WARN]
  - Both variants diff SET, no base: keep first [WARN]
- Key resolution: `performanceKey ?? key` (used in both CSV and stdout)

### `copy-gig` → `CopyGigCommand/Service`
- Clones all setlist assignments from a source gig to a new target gig slug
- Rewrites entire `setlist-assignments.csv` with TITLE and ARTIST columns enriched from the catalog (human-readable in Sheets without cross-referencing song-catalog.csv)
- Guard-rails: source gig must exist; target must not have assignments yet (unless `--force`)
- Options: `<sourceGig>` (positional), `<targetGig>` (positional), `--force`/`-f`
- Script: `./copy-gig <source-gig> <target-gig> [--force]`

### `import-new-song` → `ImportNewSongCommand/Service`
- **NOT YET IMPLEMENTED** — throws UnsupportedOperationException
- Planned: parse one new .cho and append to catalog without regenerating all
- Wiring exists (command + port interface) so implementation is a drop-in

---

## SHELL SCRIPTS (root of repo)

| Script | What |
|---|---|
| `./generate-song-catalog` | find all .cho → songsListing.txt → generate catalog |
| `./update-song` | single song catalog→.cho (edit path inside) |
| `./update-songs` | batch catalog→.cho from updateSongsListing.txt |
| `./find-song <fragment>` | grep .cho filenames by fragment, prints full path |
| `./find-song-id <fragment>` | search song-catalog.csv by title or artist; prints TITLE / ARTIST / KEY / SONG ID |
| `./list-gigs` | list all gig slugs in setlist-assignments.csv with song counts |
| `./copy-gig <src> <tgt>` | clone a gig's setlist to a new slug; rewrites assignments CSV with enriched TITLE+ARTIST |
| `./tidy-song-catalog` | strip \r from CSV (required after Google Sheets/Excel save) |
| `./fix-directive` | bulk replace `{c:` with `{comment:` in all .cho files |
| `./fix-directive-dry-run` | preview of above |
| `./copyChoSetlist` | copy all .cho to ~/tmp/setlist-ff/ (recreates dir) |
| `./copyAllSetlist` | copy all .cho + .pdf to ~/tmp/setlist-ff/ (adds to existing) |
| `./copySetlist` | hand-curated gig setlist copy script (edit per gig) |
| `./help` | show CLI help |
| `./assign-backing-track-slots` | reassign RC-500 slots for all set songs, update catalog + .cho files, regenerate setlist |

---

## CHO FILE STRUCTURE

Standard ChordPro + Pour Choices extensions in header:
```
{title: Piano Man}
{artist: Billy Joel}
{key: C}
{duration: 4:40}
{tempo: 178}
{meta: performance: A}        ← custom meta directives use {meta: key: value}
{meta: nord: M11}
{meta: countin: 4}
{meta: backing: 48}
{meta: ve: U99}
{meta: label: PianoMan}       ← RC-500 display label (max 12 chars)

{comment: Section Name}
| chord charts |

{sov}
lyrics with [Chord]inline chords
{eov}

{soc}
chorus lyrics
{eoc}
```

---

## FILESYSTEM LAYOUT

```
cho/                   # song library — alphabetical clusters
  ABC/A/Artist/        # cluster (ABC,DEF,GHI,JKL,MNO,PQR,STU,VWX,YZ)
    Song.cho
    Song-c.cho         # key variant: same song in C
pdf/                   # lead sheets / fake books (not managed by this tool)
song-catalog.csv       # THE source of truth
setlist.csv            # generated by export-setlist, not committed
updateSongsListing.txt # edit before running update-songs
songsListing.txt       # generated by generate-song-catalog script
src/main/java/com/pourchoices/chordpro/
  adapter/in/file/     # picocli @Command classes
  adapter/out/file/    # port implementations (adapters, DTOs, mappers, file I/O)
                       # Catalog: CatalogAdapter, CatalogEntryDto, CatalogEntryMapper,
                       #          CatalogFileReader, CatalogFileWriter
                       # ChordPro: ChordProAdapter, ChordProFileReader, ChordProFileWriter
                       # Setlist: SetlistAdapter, SetlistEntryDto, SetlistFileWriter
                       # SetlistAssignments: SetlistAssignmentsAdapter,
                       #   SetlistAssignmentDto, SetlistAssignmentMapper,
                       #   SetlistAssignmentsFileReader, SetlistAssignmentsFileWriter
                       # RC-500: Rc500Adapter, Rc500FileReader, Rc500FileWriter,
                       #   Rc500Mapper, Rc500SlotDto, Rc500TrackDto, Rc500AssignDto,
                       #   Rc500ParseException
                       # Misc: CustomColumnComparator, SongListingAdapter, SongListingFileReader
  application/domain/
    model/             # immutable domain objects
    service/           # business logic, implements use case interfaces
  application/port/in/ # use case interfaces
  application/port/out/# output port interfaces (CatalogPort, ChordProPort, etc.)
  config/              # ChordproCatalogIndexPathConfig → catalog-index path from properties
docs/architecture/     # command-reference.md (full sequence diagrams), generate-song-catalog.md
```

---

## HEXAGONAL ARCHITECTURE RULES

Adding a new command requires in order:
1. Domain model (if new concept) — `application/domain/model/` — @Value @Builder
2. Input port (use case interface) — `application/port/in/`
3. Service (business logic) — `application/domain/service/` — @Service @AllArgsConstructor(onConstructor_=@__(@Autowired))
4. Adapter out (if new I/O) — needs port/out interface + adapter impl
5. Picocli command — `adapter/in/file/` — @Component @Command implements Runnable
6. Register in `ChordproToolsMainCommand.subcommands`
7. Shell script at repo root (optional but conventional)
8. Test under `src/test/` mirroring package structure

Reference impl for new commands: `ExportSetlistCommand/Service` (most complete)

---

## RC-500 SONG LABEL DESIGN RULES

The RC-500 looper has a **12-character display that physically splits into two rows of 6**.
Row 1 = chars [0:6], Row 2 = chars [6:12]. There is no way to control the split — it is always at position 6.

### The core goal
Position 6 must be a **natural semantic break** so both rows read as meaningful words/phrases independently.
A label like `CoverMe` (7 chars) would display as `CoverM` / `e` — terrible. `Cover  Me` is correct.

### Three strategies (in order of preference)

1. **CamelCase capital at position 7** — cleanest, no wasted chars
   - `LaIslaBonita` → `LaIsla` / `Bonita`
   - `YouMayBeRigh` → `YouMay` / `BeRigh`
   - `LittleSister` → `Little` / `Sister`
   - `GuitarWeeps`  → `Guitar` / `Weeps`

2. **Space at position 6** — use when a complete first word is exactly 5 chars (space lands at 6)
   - `Wagon Wheel`  → `Wagon ` / `Wheel`
   - `Never Rains`  → `Never ` / `Rains`
   - `And WeDance`  → `And We` / `Dance`
   - `I Saw Light`  → `I Saw ` / `Light`

3. **Padding spaces to fill row 1** — use when first word is <5 chars; pad with spaces to reach 6
   - `Tiny  Dancer` → `Tiny  ` / `Dancer`  (2 spaces)
   - `Die   Smile`  → `Die   ` / `Smile`   (3 spaces)
   - `How   Long`   → `How   ` / `Long`    (3 spaces)
   - `Cover  Me`    → `Cover ` / `Me`      (2 spaces — first word is 5 chars)

### Anti-pattern: leading space on row 2
If the label produces a leading space on row 2, the strategy is wrong — the space belongs on row 1 as padding.
- `Takin Care` → `Takin ` / `Care` ✔  (space is trailing on row 1, not leading on row 2)
- This was caught and fixed in a post-run correction pass.

### Methodology for picking a label
1. Start from the **most recognisable word or phrase** in the song title — not necessarily the first words
   - "It Never Rains in Southern California" → `Never Rains`
   - "Just A Song Before I Go" → `BeforeIGo` (the distinctive hook)
   - "While My Guitar Gently Weeps" → `GuitarWeeps`
2. Prefer whole words over abbreviations; abbreviate only when needed to fit
   - Drop articles/prepositions first: "The", "A", "Of", "In", "And"
   - Vowel-drop for common words: `Aganst` (Against), `Diamnd` (Diamond), `Unchn` (Unchain)
   - Avoid abbreviations that destroy recognisability: `Prblm` is borderline, always prefer full second word
3. Use CamelCase to signal word boundaries when spaces are dropped
   - `RunOnEmpty`, `BadBadLeroy`, `SinceUGone`
4. Numbers are fair game and often perfect: `Jenny 8675` (everyone knows the rest)
5. Unsolvable case: **single 7-char words** — the split is always mid-word
   - `Trouble` (Travis Tritt T-R-O-U-B-L-E) → `Troubl` / `e` — unavoidable

### CamelCase label vs SET code
These are orthogonal. The label identifies the song on the RC-500 hardware *regardless of set membership*.
Both base version and all key-variant rows in the catalog carry the same label.
Key-variant detection uses `SongId.isBaseVersion()` / `KEY_ALT_PATTERN: -[a-gA-G][#b]?m?`

### Validation rule
Max 12 chars — enforced at write time (WARN log, not truncation).
All 111 labelled rows in catalog pass as of session 5.

---

## CURRENT STATE / IN-PROGRESS

- **SONG LABEL field**: Fully implemented. Column between SONG ID and SET in CSV. Written to .cho as `{meta: label: ...}`. 111 rows populated across all songs with backing tracks. Full label design rules documented in RC-500 SONG LABEL DESIGN RULES section above.
- **assign-backing-track-slots**: Fully implemented. `SetlistDeduplicator` extracted. `SetlistEntryDto` now carries BACKING column.
- **SET management**: Fully migrated. SET column removed from `CatalogEntry` and `song-catalog.csv` (Phase 3 complete). SET now lives exclusively in `setlist-assignments.csv`. `export-setlist` command is fully implemented and joins both CSVs at runtime.
- **setlist-assignments.csv enrichment (session 10)**: `SetlistAssignmentDto` now carries TITLE and ARTIST as decorative columns. The reader ignores them if absent (backward compatible). `SetlistAssignmentsPort.writeEnrichedAssignments()` populates them from the catalog. `copy-gig` always writes enriched rows so the file is self-readable in Sheets.
- **copy-gig (session 10)**: Fully implemented. Clones any gig's assignments to a new slug, with `--force` to overwrite. Writes enriched CSV.
- **find-song-id (session 10)**: Shell script. Searches song-catalog.csv by title or artist fragment; prints TITLE / ARTIST / KEY / SONG ID table. Python-based, no Java required.
- **list-gigs (session 10)**: Shell script. Prints all gig slugs with song counts from setlist-assignments.csv.
- **Sets work in flight**: Several new commands related to set management are planned but not yet started (beyond export-setlist).
- **import-new-song**: Stubbed, throws UnsupportedOperationException.
- **copySetlist** (shell script): Still maintained manually. Goal is to eventually drive it entirely from SET column via export-setlist.
- **No export-setlist shell script yet**: README documents manual mvn invocation; script creation is a noted TODO.

---

## WORKFLOW (normal session)

1. Edit `song-catalog.csv` in Google Sheets or Excel
2. Save CSV → run `./tidy-song-catalog` to strip \r
3. `./update-song` or `./update-songs` to push metadata into .cho files
4. `export-setlist` to generate setlist.csv for gig night
5. `./copyChoSetlist` or `./copySetlist` to stage files for OnSong

Adding new songs:
1. Drop .cho file into appropriate `cho/CLUSTER/LETTER/Artist/` directory
2. Run `./generate-song-catalog` to rebuild catalog (loses any un-pushed SET edits)
   OR (when implemented) `import-new-song` to append single song

---

## CHO FILE LINTING (session 9)

- `lint-cho.zsh` — standalone zsh script at project root.
- **Modes:** `--check` (default, exit 1 on violations, safe for CI/pre-commit) | `--fix` (in-place correction, exit 0)
- **Target:** `./cho` by default; accepts a file or directory argument.
- **Rules** (defined once as parallel FROM/TO arrays, grep pattern + perl sub derived from them):
  - `{soc}/{eoc}` → `{start_of_chorus}/{end_of_chorus}`
  - `{sov}/{eov}` → `{start_of_verse}/{end_of_verse}`
  - `{sob}/{eob}` → `{start_of_bridge}/{end_of_bridge}`
  - `{sot}/{eot}` → `{start_of_tab}/{end_of_tab}`
  - `{sog}/{eog}` → `{start_of_grid}/{end_of_grid}`
  - `{start_of_tabs}/{end_of_tabs}` → `{start_of_tab}/{end_of_tab}` (typo fix)
- **Intentionally excluded:** `{start_of_part}/{end_of_part}` (custom extension OnSong handles), parameterised grid directives e.g. `{start_of_grid 4x4+1}` (already full-form).
- **Status:** 201 of 488 files corrected; `--check` now exits 0 across entire corpus.
- **Future:** Java `normalize-cho-files` command to enforce same rules within the Spring CLI (explicit, not auto-hooked).

- `generate-song-catalog` was designed for **batch ingestion** (adding/updating many songs at once). Scott has noted a future goal: a discrete `add-song-to-catalog` command that imports a single new song without full re-ingestion. Keep this in mind for future sessions.
- `generate-song-catalog` CLEARS columns not in .cho headers (PERFORMANCE KEY etc.) — always push edits first via update-songs. Now safe for SET since SET is fully removed from the catalog (lives only in setlist-assignments.csv).
- **DATA MODEL DECISION (session 7):** Full analysis in `docs/architecture/data-storage-options.md`. Summary:
  - CSVs ARE the database. No binary DB in Git (kills diffs + Google Sheets editability).
  - **Planned:** split into `song-catalog.csv` (song identity + hardware) and `setlist-assignments.csv` (gig planning). Join on SONG ID at runtime in Java. **PHASE 1 COMPLETE (session 8).**
  - `setlist-assignments.csv` columns: `GIG, SONG ID, SET` — one row per song-in-gig assignment (long/tidy format). GIG is a date-first slug e.g. `2026-06-14-rusty-nail`. Multiple gigs = multiple rows sharing the same GIG value. Filter by GIG in the service layer.
  - Seeded with 2 placeholder rows (gig=`tbd`). User to recreate real gig data from past setlists.
  - New infrastructure: `SetlistAssignment` (domain), `SetlistAssignmentDto`, `SetlistAssignmentMapper`, `SetlistAssignmentsPort` (out), `SetlistAssignmentsFileReader`, `SetlistAssignmentsFileWriter`, `SetlistAssignmentsAdapter`, `ChordproSetlistAssignmentsPathConfig`.
  - Property: `chordprotools.setlist-assignments=./setlist-assignments.csv`
  - **Phase 2 COMPLETE (session 9):** pivot setlist services (ExportSetlistService, AssignBackingTrackSlotsService, SetlistDeduplicator) to join song-catalog + setlist-assignments on SONG ID.
    - `SetlistJoiner` (@Component) added: resolves gig, joins assignments → catalog, returns `List<SetlistEntry>`.
    - `SetlistEntry` domain object added: wraps `CatalogEntry` + `SetlistAssignment` with delegate accessors.
    - `Setlist` domain object extended: now carries `gig: String` + `entries: List<SetlistEntry>` (was bare `List<CatalogEntry>`).
    - Both `ExportSetlistService` and `AssignBackingTrackSlotsService` now use `SetlistJoiner`; both accept `--gig` / `-g` option.
  - **Phase 3 COMPLETE (session 8):** SET fully removed from the catalog side.
    - `CatalogEntry`: `String set` field removed.
    - `HeaderDirective`: `SET` enum constant removed.
    - `CatalogEntryDto`: `"set"` removed from CATALOG_COLUMN_ORDER + `@CsvBindByName` field removed. Now 15 columns.
    - `CatalogEntryMapper`: `.set()` calls removed from both `toEntity()` and `toDto()`.
    - `CatalogEntryToParsedHeaderMapper`: `addIfPresent(SET, ...)` call removed.
    - `GenerateSongCatalogService`: `else if SET builder.set(v)` branch removed.
    - `CatalogEntryMapperTest`: `.set()` builder calls + `assertThat(set)` assertions removed.
    - `song-catalog.csv`: SET column stripped (487 rows, now 15 columns).
    - `MyLife-c.cho`: `{meta: Set: B10}` + mirror comment line removed.
    - `CarelessWhisper.cho`: `{meta: Set: C08}` + entire comment block removed (was the only meta directive on that song).
    - Tests: 61 passing. SET is gone from the catalog entirely.
  - SET should eventually leave HeaderDirective + .cho files entirely (gig data ≠ musical data).
  - H2/SQLite rejected: binary formats, non-editable in Sheets, no capability gain at 500-row scale.
  - Google Sheets stays as the editing surface — hard constraint, guitarist must be able to edit without tooling.
- Catalog key is SongId string (e.g. `ABC:B:BillyJoel:MyLife`), NOT file path
- `tidy-song-catalog` MUST be run after any spreadsheet save before update-song/update-songs
- Key variant files (`Song-c.cho`) matched by regex `-[a-gA-G][#b]?m?` — non-key suffixes like `-old`, `-MVP` are NOT variants
- `BACKING` value `99` = "no backing track" sentinel
- `COUNTIN` value `24` = default (no count-in)
- `SONG LABEL` max 12 chars (RC-500 hardware limit) — mapper logs WARN if exceeded, does NOT truncate
- `NORD` / `VE` null value sentinel = `"null"` string
- Java version in pom: 21 (README says 17 — pom wins, it's 21)
