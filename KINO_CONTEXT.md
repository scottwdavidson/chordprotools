# KINO_CONTEXT — chordprotools
# Canonical project brain, maintained by whatever coding agent is working this repo.
# (Filename is historical — "Kino" was an earlier agent. You inherit this doc; keep it current.)
# Update end-of-session whenever code, schemas, commands, or decisions change. Not for human consumption.
# Last updated: 2026-08-29 (session 19)
# See also: PRODUCT_PLAN.md — the living launch-readiness checklist + roadmap doc.
#           Phase 1 = Scott personally verifies every core feature (RC-500 hardware
#           pipeline flagged as highest-risk/least-verified). Phase 2 = roadmap,
#           built from Phase 1's issue log + idea parking lot. Check its status
#           before assuming a feature is "done" — built and tests-green does not
#           necessarily mean gig-verified.
#           key-aware-transposition-and-drift-verification.md — approved design for a
#           `transpose` command + `verify-sync`/`consistent-song-data` drift detection.
#           Phase 0 (harden ChordProTransposer: slash chords, non-chord-bracket guard,
#           key-driven spelling) must land before transpose/verify-sync are built.

---

## WHAT THIS IS

CLI tool for the **Pour Choices band** (pourchoicesmusic.com) to manage ~500 ChordPro (`.cho`) song files and the metadata that drives live gigs. Extends standard ChordPro headers with band-specific fields for hardware presets (Nord piano, Roland keyboard, VE-500 vocal effects, RC-500 looper backing tracks, BeatBuddy count-ins) and set management.

**Two primary data sources (both at repo root):**
- `song-catalog.csv` — stable song library, one row per `.cho` file
- `gigs.csv` — per-gig assignments, one row per song-in-gig (formerly `setlist-assignments.csv`)

**Song files:** `cho/**/*.cho` — consumed by OnSong on stage

They are kept deliberately separate: the catalog is long-lived and stable; setlists change freely gig to gig without touching song metadata.

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
PERFORMANCE KEY, TIME SIGNATURE, SONG ID, SONG LABEL
```

(14 columns. RC SLOT was removed in session 12 — it now lives exclusively in `gigs.csv`. SET was removed in Phase 3. **CAPO was removed in session 16** — it was the 15th column, sat between TIME SIGNATURE and SONG ID, and is now gone from catalog, `.cho` files, and the domain model.)

- **SONG ID**: structured key `CLUSTER:LETTER:ArtistDir:SongStem[-keyVariant]`
  - e.g. `ABC:B:BillyJoel:MyLife` or `ABC:B:BillyJoel:MyLife-c`
  - Maps 1:1 to filesystem path under `./cho/`
- **SONG LABEL**: RC-500 display label for click/backing tracks — **max 12 chars** (hardware limit). Placed between SONG ID and SET. Written to .cho as `{meta: label: ...}`. Logs WARN on read if > 12 chars.
- **SET**: sortable alphanumeric code e.g. `A01`, `A02`, `B01` — drives `export-setlist`
- **PERFORMANCE KEY**: key band actually plays in (may differ from chart key in `.cho` file)
- **COUNTIN**: count-in source (e.g. `4` = BeatBuddy, `8` = backing track, `24` = default)
- **BACKING**: device type — `RC` (RC-500 looper), `BB` (BeatBuddy drummer), or blank (no backing). NOT a slot number — the slot lives in `gigs.csv`.
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
Delegate methods: `getSet()`, `getGig()`, `getSongId()`, `getTitle()`, `getArtist()`, `getKey()`, `getPerformanceKey()`, `getBackingType()`.
`getBacking()` returns: `"BB"` for BeatBuddy songs, the RC-500 slot from the gig assignment for RC songs, `""` for no backing.
This is the primary unit of currency for all setlist-producing services.

### SetlistJoiner (@Component)
Resolves gig slug (explicit `--gig` param or falls back to lexicographically last GIG value in assignments),
filters `SetlistAssignment` rows to that gig, joins with the catalog map, and returns `List<SetlistEntry>`.
Shared by `ExportSetlistService` and `AssignBackingTrackSlotsService`.
Key methods: `join(gigParam, allAssignments, catalog)`, `resolveGig(gigParam, allAssignments)`.
**Failure behaviour (session 11):** `buildEntry()` throws `IllegalStateException` if a SONG ID from assignments is absent from the catalog. A missing base version is a data-integrity violation — fails loudly, never silently skips. (Was: `Optional.empty()` + warning-and-skip.)

### SetlistAssignment (@Value @Builder(toBuilder = true))
One row in `gigs.csv`. Fields: `gig` (slug), `songId` (SongId), `set` (position code), `rcSlot` (String, nullable — blank until `assign-backing-track-slots` runs for this gig; never copied by `copy-gig`).
**Invariant (session 11):** `songId` must always be a base version — `SongId.isBaseVersion()` must be true. Variant IDs (e.g. `MyLife-c`) are illegal in assignments. Enforcement: `SetlistAssignmentMapper.toEntity()` validates on read and throws `IllegalArgumentException` with the suggested fix if a variant is found.

### Setlist (@Value @Builder)
`gig: String` + `entries: List<SetlistEntry>` + `size()`. Produced by both setlist services, consumed by CLI commands for stdout rendering and summary output.

---

## COMMANDS

### `import-song` → `ImportNewSongCommand/ImportNewSongService`
- Registers a new `.cho` file in `song-catalog.csv` without touching any other rows
- SONG ID is derived from the file path via `ChordProPath.toSongId()` — user never constructs it manually
- Options: positional path arg, `--dry-run`/`-n` (preview without writing)
- Guards: throws if SONG ID already exists in catalog; throws if file doesn't exist
- Script: `./import-song <path> [--dry-run]`
- **Replaces the deleted `generate-song-catalog` for single-song ingestion**

### `verify-catalog` → `VerifyCatalogCommand/VerifyCatalogService`
- Reads every row in `song-catalog.csv`, opens the corresponding `.cho` file, compares field by field
- Reports `[MISSING FILE]` (file not on disk) and `[DRIFT]` (field mismatch) per affected row
- **RC SLOT is intentionally excluded** from comparison — it is a per-gig property owned by `gigs.csv`
- Script: `./verify-catalog`
- Returns summary: `verify-catalog: N clean, M issue(s) found`

### `update-song` → `UpdateSongCommand/Service`
- Catalog → one .cho file: reads catalog, finds entry by SongId, maps to ParsedHeader, compares to current file, writes only if changed
- Body (chords/lyrics) is preserved entirely
- **RC SLOT preservation (session 12):** `withPreservedRcSlot()` helper injects any existing `{meta: rc-slot: N}` from the current file into the catalog-derived header before comparison — a slot assigned by `assign-backing-track-slots` is never erased by a catalog update
- Script: `./update-song <path>` (path as argument)
- Catalog key: SongId string derived from file path via `ChordProPath.toSongId()`

### `update-songs` → `UpdateSongsCommand/Service`
- Batch version of update-song: reads updateSongsListing.txt, calls UpdateSongService per file
- Script: `./update-songs`
- Edit `updateSongsListing.txt` with one .cho path per line

### `assign-backing-track-slots` → `AssignBackingTrackSlotsCommand/Service`
- Loads `song-catalog.csv` + `gigs.csv`, resolves target gig via `SetlistJoiner`, deduplicates via `SetlistDeduplicator`, splits into in-set (A–Y prefix) vs backup (Z prefix)
- Assigns RC-500 slot numbers for all gig-assigned **RC** songs only (BB and blank are skipped)
- **In-set songs** (SET prefix A–Y): sorted by SET code, slots assigned from **5** upward
- **Backup songs** (SET prefix Z): sorted alphabetically by title, slots from **50** upward (hard cap at 99)
- **Side-effects (session 12 — catalog no longer touched):**
  1. Write RC SLOT values for this gig into `gigs.csv` (only this gig’s rows; other gigs untouched)
  2. `patchRcSlotInFile()` patches `{meta: rc-slot: N}` directly into each affected `.cho` file (strips old RC_SLOT line, adds new one via ParsedHeader builder)
  3. Re-join with updated assignments → write fresh `setlist.csv`
- Options: `--gig`/`-g` (slug; auto-resolves to latest gig if omitted), `--output`/`-o` (default `./setlist.csv`)
- Constants: `IN_SET_START_SLOT=5`, `BACKUP_START_SLOT=50`, `MAX_SLOT=99`
- Dependencies: `CatalogPort`, `SetlistPort`, `SetlistAssignmentsPort`, `ChordProPort`, `SongParser`, `SetlistDeduplicator`, `SetlistJoiner`

### `export-setlist` → `ExportSetlistCommand/Service`
- Loads `song-catalog.csv` + `gigs.csv`, resolves target gig, joins via `SetlistJoiner`, deduplicates, sorts by SET code, writes `setlist.csv`
- Prints formatted table to stdout (SET / TITLE / ARTIST / KEY / BACKING)
- **Default (session 13): fan-facing only** — Z-set backup songs excluded from both CSV and stdout
- **`--verbose`/`-v`**: includes Z-set songs; verbose console table shows a `BACKUP / Z-SET` section separator before the first Z entry
- `ExportSetlistUseCase.exportSetlist(gigParam, outputPath, includeBackup)` — the `includeBackup` boolean drives the filter in `ExportSetlistService`
- Options: `--gig`/`-g`, `--output`/`-o` (default `./setlist.csv`), `--verbose`/`-v` (default false)
- Key resolution: `performanceKey ?? key` (used in both CSV and stdout)
- `BACKING` column: slot number (from gig’s `gigs.csv` RC SLOT) for RC songs, `"BB"` for BeatBuddy, blank otherwise
- **Throws** `IllegalStateException` (via `SetlistJoiner`) if any assigned SONG ID is absent from the catalog

### `copy-gig` → `CopyGigCommand/Service`
- Clones all setlist assignments from a source gig to a new target gig slug
- Rewrites entire `gigs.csv` with TITLE and ARTIST columns enriched from the catalog
- **RC SLOT is never copied** — new gig’s rows always start blank; fresh assignment done via `assign-backing-track-slots`
- Guard-rails: source gig must exist; target must not have assignments yet (unless `--force`)
- Options: `<sourceGig>` (positional), `<targetGig>` (positional), `--force`/`-f`
- Script: `./copy-gig <source-gig> <target-gig> [--force]`

### `consistent-metadata` → `ConsistentMetadataCommand/Service` (session 16)
- Scans the **whole catalog** for key-variant inconsistencies across songs that have 2+ variants (grouped via `SongId.toGroupKey()`)
- **Two checks:**
  1. `[DRIFT]` — any non-KEY field differs between variants of the same song (duration, tempo, count-in, backing, label, NORD, ROLAND, VE, **performance key**, time sig, …). Performance key MUST match across variants.
  2. `[FILENAME/KEY]` — when a variant's filename encodes a key (`-b`, `-c#m`, `-am`), the `{key:}` inside the file must match the key in the filename.
- **Dry-run by default.** `--fix` repairs DRIFT by propagating metadata from a source-of-truth variant into `song-catalog.csv`. FILENAME/KEY mismatches are **never** auto-fixed (must be fixed by hand).
- `--source <SONG ID>`: variant to treat as source of truth when fixing (defaults to each group's base/standard-key variant)
- **Scriptable exit code** = number of issue groups (0 = clean)
- Domain: `MetadataConsistencyReport` (with nested `Finding` + `FindingType` enum DRIFT/FILENAME_KEY), `CatalogEntryComparator`
- Use case: `ConsistentMetadataUseCase.check(boolean fix, String sourceSongId)`
- Script: `./consistent-metadata [--fix] [--source <SONG ID>]`
- Design doc: `consistent-metadata.md` at repo root

---

## SHELL SCRIPTS (root of repo)

**Execution model (session 15):** Java-backed shims no longer call `mvn spring-boot:run` (slow, ~5-10s). They delegate to `./cpt`, which runs the packaged fat JAR directly via `java -jar` (<1s). `./cpt` **auto-rebuilds** (never goes stale) if the JAR is missing OR if any file under `src/main` or `pom.xml` is newer than the JAR — so a `git pull` that changes code/resources/deps is picked up automatically on the next command. `./build` does `mvn package -DskipTests`. Rebuild messages go to stderr (stdout stays clean for piped CSV output).

| Script | What |
|---|---|
| `./build` | compile + package fat JAR (`mvn package -DskipTests`); run after code changes. **Auto-selects Maven settings by VPN reachability (session 16):** `nc -z mvn.ci.artifacts.walmart.com 443` → on-VPN uses `~/.m2/settings-walmart.xml`, off-VPN uses `~/.m2/settings-central.xml`; falls back to Maven defaults if neither exists. Override with `MVN_FORCE=walmart` / `MVN_FORCE=central`. |
| `./cpt <cmd> [args]` | internal launcher: `java -jar target/chordpro-tools-*.jar`; auto-rebuilds if JAR missing or any src/main file or pom.xml is newer than JAR (never stale). `./cpt --version` prints git-tag version + commit + build time (session 16). |
| `./import-song <path> [--dry-run]` | register a new .cho file in song-catalog.csv |
| `./verify-catalog` | check all catalog entries against their .cho files; report MISSING FILE / DRIFT |
| `./update-song <path>` | single song catalog→.cho |
| `./update-songs` | batch catalog→.cho from updateSongsListing.txt |
| `./find-song <fragment>` | grep .cho filenames by fragment, prints full path |
| `./find-song-id <fragment>` | **Java command** (find-song-id). Search song-catalog.csv by title or artist; ONE row per song (base version), annotates `+N key variants`; orphan variants (no base) flagged `[!]`; SONG ID column is always a valid gigs.csv foreign key; exit 1 if no match |
| `./list-gigs` | **Java command** (list-gigs). List all gig slugs in gigs.csv with song counts, sorted chronologically by slug; reuses SetlistAssignmentsPort |
| `./copy-gig <src> <tgt>` | clone a gig's setlist to a new slug; rewrites gigs.csv with enriched TITLE+ARTIST; RC SLOT always blank on new rows |
| `./export-setlist [--verbose]` | generate setlist.csv for the latest (or `--gig`) gig; default excludes Z-sets (fan setlist); `--verbose` includes backup songs |
| `./tidy-song-catalog` | strip \r from song-catalog.csv (required after Google Sheets/Excel save) |
| `./tidy-gigs` | strip \r from gigs.csv (required after Google Sheets/Excel save) |
| `./fix-directive` | bulk replace `{c:` with `{comment:` in all .cho files |
| `./fix-directive-dry-run` | preview of above |
| `./copyChoSetlist` | copy all .cho to ~/tmp/setlist-ff/ (recreates dir) |
| `./copyAllSetlist` | copy all .cho + .pdf to ~/tmp/setlist-ff/ (adds to existing) |
| `./copySetlist` | hand-curated gig setlist copy script (edit per gig) |
| `./help` | show CLI help |
| `./assign-backing-track-slots [--gig]` | assign RC-500 slots for the gig → write `gigs.csv` + patch `.cho` files + regenerate `setlist.csv` |
| `./consistent-metadata [--fix] [--source <id>]` | **Java command** (session 16). Check key-variants of each song share consistent catalog metadata (everything but KEY, incl. performance key); flags filename/key mismatches. Dry-run unless `--fix`. Exit code = issue-group count |
| `./deploy-rc500 [--gig] [--source] [--target] [--output-dir]` | Java-backed command. Generates `deploy-rc500-<timestamp>.sh` with plain `cp` commands; config via `application.properties` or CLI flags |

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
docs/guides/           # chordpro-style-usage.md (band conventions for writing .cho files)
song-catalog.csv       # THE song library source of truth (535 rows, 14 columns)
gigs.csv               # per-gig setlist assignments (GIG, SONG ID, SET, RC SLOT)
setlist.csv            # generated by export-setlist, not committed
updateSongsListing.txt # edit before running update-songs
src/main/java/com/pourchoices/chordpro/
  adapter/in/file/     # picocli @Command classes
  adapter/out/file/    # port implementations (adapters, DTOs, mappers, file I/O)
                       # Catalog: CatalogAdapter, CatalogEntryDto (14 cols), CatalogEntryMapper,
                       #          CatalogFileReader, CatalogFileWriter
                       # ChordPro: ChordProAdapter, ChordProFileReader, ChordProFileWriter
                       # Setlist: SetlistAdapter, SetlistEntryDto, SetlistFileWriter
                       # Gigs: SetlistAssignmentsAdapter (reads/writes gigs.csv),
                       #   SetlistAssignmentDto (cols: gig, song id, set, rc slot),
                       #   SetlistAssignmentMapper, SetlistAssignmentsFileReader,
                       #   SetlistAssignmentsFileWriter
                       # RC-500: Rc500Adapter, Rc500FileReader, Rc500FileWriter,
                       #   Rc500Mapper, Rc500SlotDto, Rc500TrackDto, Rc500AssignDto,
                       #   Rc500ParseException
                       # Misc: CustomColumnComparator, SongListingAdapter, SongListingFileReader
  application/domain/
    model/             # immutable domain objects (BackingType enum, CatalogEntry,
                       #   SetlistAssignment, SetlistEntry, Setlist, SongId, ParsedSong, ...)
    service/           # business logic, implements use case interfaces
  application/port/in/ # use case interfaces
  application/port/out/# output port interfaces (CatalogPort, ChordProPort, etc.)
  config/              # ChordproCatalogIndexPathConfig → catalog-index path from properties
docs/architecture/     # command-reference.md (full sequence diagrams), data-storage-options.md,
                       #   generate-song-catalog.md, audio-workflow.md (verify-hardware DESIGN doc)
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

### Completed (fully implemented, tests green)
- **Catalog growth (session 18)**: catalog now **538 song rows / 538 `.cho` files** (was 507/512 at session 16 — grew to 535/539 mid-session before the 4-orphan reconciliation below brought it back to 1:1). Bulk of session 17→18 activity was bandmate PRs adding/fixing songs (America, Cheap Trick, Christopher Cross, Crowded House, Duran Duran rewrite, Eric Clapton, George Harrison, Gin Blossoms, Joe Cocker, John Lennon, Kane Brown, King Harvest, Luke Combs, Prince, Psychedelic Furs, Red Hot Chili Peppers, Velvet Revolver, and more) plus routine tidy/fix commits — no domain-model changes. Two file renames worth knowing: `ClintBlack/BetterMan.cho` → `ABetterMan.cho`, `PsychedelicFurs/GhostInYou.cho` → `TheGhostInYou.cho` (SONG ID changes accordingly — re-run `./find-song-id` if a gig references the old ID). See the reconciliation entry directly below for how the 4-file catalog/filesystem gap was closed.
- **New docs (session 18)**: `docs/architecture/audio-workflow.md` — a DESIGN-ONLY spec for a future `verify-hardware` command (SHA-256 manifest-based integrity check between Google Drive masters and files streamed live off the mounted RC-500 over USB); nothing here is built, no picocli command exists yet. `docs/guides/chordpro-style-usage.md` — band-facing (not code) style guide for writing `.cho` bodies: full `{start_of_part: Name}`/`{end_of_part}` directives (OnSong doesn't reliably parse abbreviations), leading-period measure-grid syntax for instrumental blocks, and two riff-notation conventions (literal note names vs. Nashville-number scale degrees). Both docs are cross-linked from `README.md` via new `<a id=...>` anchors (session 18) — no functional README changes.
- **tidy-gigs command upgrade (session 17)**: Upgraded from bash `sed` script to a PicoCLI command (`TidyGigsCommand`/`Service`/`UseCase`). Strips `\r` and sorts `gigs.csv` by GIG then SET. Includes JUnit tests and uses `chordprotools.gigs` config property to prevent hardcoded overwrites in tests.
- **SONG LABEL field**: Column between SONG ID and CHORDPRO FILENAME in catalog. Written to .cho as `{meta: label: ...}`. 111 rows populated for all songs with backing tracks. Design rules documented in RC-500 SONG LABEL DESIGN RULES section.
- **assign-backing-track-slots**: Fully implemented. `SetlistDeduplicator` component extracted. `SetlistEntryDto` carries BACKING column.
- **SET migration (Phase 3 complete)**: SET column removed from `CatalogEntry` and `song-catalog.csv`. SET lives exclusively in `gigs.csv`. `export-setlist` joins both CSVs at runtime.
- **export-setlist**: Fully implemented, including `./export-setlist` shell script.
- **gigs.csv enrichment (session 10, formerly setlist-assignments.csv)**: `SetlistAssignmentDto` carries TITLE and ARTIST as decorative/human-readable columns. Backward-compatible reader. `copy-gig` always writes enriched rows.
- **copy-gig (session 10)**: Fully implemented. Clones any gig’s assignments to a new slug, with `--force` to overwrite.
- **find-song-id (sessions 10–11; converted to Java session 15)**: Shows ONE row per song (base version only). Annotates `+N key variants`. SONG ID column is always a valid, pasteable gigs.csv foreign key. (See session-15 entry below — now a Java command, not Python.)
- **list-gigs (session 10; converted to Java session 15)**: Prints all gig slugs with song counts from gigs.csv. (See session-15 entry below — now a Java command, not Python.)
- **Option B — base-version-only SONG IDs in assignments (session 11)**: Enforced by `SetlistAssignmentMapper.toEntity()`. `SetlistJoiner.buildEntry()` throws on missing catalog entry. `SetlistDeduplicator` simplified to lean duplicate-song guard.
- **README rewrite (sessions 11, 13)**: Fully updated to reflect current architecture.
- **lint-cho.zsh (session 9)**: Standalone linter. 201/488 files corrected; `--check` exits 0 across full corpus.
- **RC SLOT moved to gigs.csv (session 12)**: `CatalogEntry.rcSlot` removed. `SetlistAssignment.rcSlot` added (nullable). `SetlistAssignmentDto` gains `rc slot` column. `SetlistEntry.getBacking()` returns `"BB"` | slot-from-assignment | `""`. `UpdateSongService.withPreservedRcSlot()` prevents `update-song` from erasing gig-assigned slots. `AssignBackingTrackSlotsService` rewritten: no catalog writes, patches `.cho` files directly. `song-catalog.csv` migrated (16→15 cols). `gigs.csv` migrated (3→4 cols). 69 tests passing.
- **import-song (session 12)**: Fully implemented. Replaces deleted `generate-song-catalog` for single-song ingestion. Supports `--dry-run`.
- **verify-catalog (session 12)**: Fully implemented. Reports MISSING FILE and DRIFT per row. RC SLOT intentionally excluded from comparison.
- **export-setlist --verbose (session 13)**: Default output excludes Z-set backup songs (fan setlist). `--verbose`/`-v` flag includes backup; adds a visual section separator. `ExportSetlistUseCase.exportSetlist()` gains `includeBackup` boolean.
- **deploy-rc500 (session 14, refactored session 14)**: Java picocli command (`GenerateRc500DeployScriptCommand` / `GenerateRc500DeployScriptService`) — NOT a shell script. Generates `deploy-rc500-<timestamp>.sh` containing pure `cp` commands (no `mkdir`, no logic) so user can review, trim, and run. Key-variant stripped via `SongId.getTitle()` (already the base title). Assignments sorted by RC slot number. `backing.wav` missing at generation time → `⚠ WARNING` comment block, `cp` commented out. `click.wav` missing → `# INFO` comment, line omitted. No RC slot → silently skipped. Config: `chordprotools.backing-source-root` + `chordprotools.rc500-target-root` in `application.properties` (blank defaults, machine-specific); `--source`/`--target` CLI flags override. Options: `--gig`/`-g`, `--source`/`-s`, `--target`/`-t`, `--output-dir`/`-o`. Generated scripts are gitignored (`deploy-rc500-*.sh`). `ChordproRc500Config` is the new config bean. Shell script `deploy-rc500` is a 3-line shim like all other commands.
- **Fast command execution / build+cpt launcher (session 15)**: Created `./build` (`mvn package -DskipTests`, ~4s) and `./cpt` (internal launcher running `java -jar target/chordpro-parser-*.jar`, <1s vs ~5-10s for `mvn spring-boot:run`). `./cpt` **auto-rebuilds (never goes stale)**: `needs_build()` returns true if no JAR or if `find "$DIR/src/main" "$DIR/pom.xml" -newer "$jar"` finds anything; on true it runs `./build` then re-resolves the JAR; otherwise `exec java -jar`. Rationale: bandmate does `git pull` and may not realise code changed — auto-rebuild prevents running stale code. All build/rebuild chatter on stderr (`>&2`) so stdout stays clean for piped CSV. All 9 Java-backed shims (`verify-catalog`, `update-songs`, `update-song`, `export-setlist`, `import-song`, `copy-gig`, `assign-backing-track-slots`, `deploy-rc500`, `help`) rewritten as 1-line delegations: `"${0:a:h}/cpt" <command> "$@"`. JAR name resolved by glob (version-agnostic), filtering `.original`. `target/` already gitignored. NOT touched: `tidy-*`/`fix-*`/`copy*Setlist`/`lint-cho.zsh`/`find-song` (pure shell), `update-catalog` (references unregistered command, likely dead). (`find-song-id` + `list-gigs` were converted to Java later this session — see entries below.) Verified: fast path ~0.75s; touching a .java or pom.xml triggers one rebuild then fast path restored (no loop).
- **list-gigs → Java (session 15)**: Converted from inline-Python shell script to a picocli command. New files: `ListGigsUseCase` (port/in), `ListGigsService` (domain/service), `ListGigsCommand` (adapter/in/file), `GigSummary` (domain/model immutable result), `ListGigsServiceTest` (4 tests). Service reuses `SetlistAssignmentsPort.readAssignments()` (same reader as every other gig command) instead of parsing gigs.csv directly with Python's csv module. Logic: count assignments per `gig` field (TreeMap for sorted-by-slug), map to `GigSummary`. Command prints `No gigs found in gigs.csv` when empty, else a formatted table. Shell script now a `cpt` shim. Live parity verified against raw CSV counts (32/59/59/59/62).
- **find-song-id → Java (session 15)**: Converted from inline-Python shell script to a picocli command. New files: `FindSongIdUseCase` (port/in), `FindSongIdService` (domain/service), `FindSongIdCommand` (adapter/in/file), `SongMatch` (domain/model immutable result), `FindSongIdServiceTest` (7 tests). Service reuses `SongId.toGroupKey()` + `SongId.isBaseVersion()` instead of re-implementing the key-variant regex the Python had (kills a DRY violation — the regex existed in BOTH Java and Python). Logic: bucket catalog by group key → {base, variants[]}; filter by case-insensitive title/artist fragment; promote first variant as representative + flag `orphan` if no base; sort by title. Command does table formatting + `System.exit(1)` on no match. Shell script now a `cpt` shim. NOTE: `-old` suffix (e.g. `PianoMan-old`) is NOT a key variant (regex only matches `-[a-gA-G][#b]?m?`), so it surfaces as its own base row — identical to old Python behavior.
- **Clean stdout / logging (session 15)**: Added `spring.main.banner-mode=off` + `logging.level.root=WARN` to `application.properties`. Previously every Java command dumped the Spring banner + INFO logs onto stdout, which would corrupt piping (`find-song-id ... | grep`). Now stdout carries only command output. Re-enable diagnostics per-run with `-Dlogging.level.com.pourchoices.chordpro=DEBUG`.
- **CAPO removed from catalog + song data (session 16)**: CAPO was the 15th catalog column (between TIME SIGNATURE and SONG ID). Removed everywhere: `CatalogEntry.capo` field, `HeaderDirective.CAPO`, `CatalogEntryDto` (`"capo"` dropped from `CATALOG_COLUMN_ORDER` + `@CsvBindByName` field), `CatalogEntryMapper`, `CatalogEntryToParsedHeaderMapper`/`ParsedHeaderToCatalogEntryMapper`, `CatalogEntryComparator`, `ConsistentMetadataCommand/Service/UseCase`, `MetadataConsistencyReport`. `song-catalog.csv` migrated (15→14 cols across all ~507 rows). `{capo:}` directives stripped from `.cho` files (e.g. `WatchingTheWheels.cho`, `FreeFallin.cho`). README + consistent-metadata.md + consistent-song-data.md + data-storage-options.md + stabilize-song-body-delimiter.md updated. Tests adjusted (CatalogEntryMapperTest, ConsistentMetadataServiceTest). Rationale: capo is a player-side performance choice, not durable song identity — and it muddied cross-variant consistency checks.
- **consistent-metadata command (session 16)**: Fully implemented. Detects (and `--fix` repairs) cross-variant catalog DRIFT + filename/key mismatches across songs with 2+ key-variants. New files: `ConsistentMetadataUseCase` (port/in), `ConsistentMetadataService` (domain/service), `ConsistentMetadataCommand` (adapter/in/file), `MetadataConsistencyReport` + nested `Finding`/`FindingType` (domain/model), `CatalogEntryComparator` (domain/service). Dry-run by default; `--fix` propagates metadata from a source-of-truth variant (`--source`, defaults to base variant) into `song-catalog.csv`; FILENAME/KEY issues never auto-fixed. Exit code = issue-group count. Design doc: `consistent-metadata.md`. Registered in `ChordproToolsMainCommand.subcommands`; shell shim `./consistent-metadata` delegates via `cpt`.
- **Git-tag versioning + `--version` (session 16)**: `pom.xml` adds `io.github.git-commit-id:git-commit-id-maven-plugin` (`revision` goal) to bake commit/build metadata into the JAR. New `GitVersionProvider` (adapter/in/file) implements picocli `IVersionProvider`; wired into `ChordproToolsMainCommand` so `./cpt --version` prints `chordpro-tools v<git-tag>` + short commit + ISO build time (e.g. `v1.0.0-dirty`, `commit: 61b5092`).
- **VPN-aware build (session 16)**: `./build` auto-selects Maven settings since scripts don't source `~/.zshrc`. `nc -z -G2 mvn.ci.artifacts.walmart.com 443` → reachable uses `~/.m2/settings-walmart.xml` (Walmart Artifactory), unreachable uses `~/.m2/settings-central.xml` (Maven Central); neither present → Maven default settings. Override: `MVN_FORCE=walmart` / `MVN_FORCE=central`. Lets bandmates build off-VPN on non-Walmart machines.
- **Catalog growth (session 16)**: catalog now ~507 song rows / 512 `.cho` files (was 487). New songs added by bandmates via PRs (Black Crowes, The Cure, Gin Blossoms, Seal, New Radicals, Luke Combs, Tainted Love, etc.). Several commits are titled "new, not validated" — this means the **song content itself** (chords/lyrics) hasn't been vetted/learned yet, so those songs are nowhere near gig-ready. It is NOT a reference to `verify-catalog`/`consistent-metadata` tooling status.

- **Catalog fully synced (session 18)**: Ran the 4-orphan reconciliation flagged above. `song-catalog.csv` and `cho/**/*.cho` are now in 1:1 lockstep at **538 rows / 538 files**, `./verify-catalog` reports 0 MISSING FILE issues (2 pre-existing DRIFT issues remain on `ThatThingYouDo`/`ThatThingYouDo-a`, unrelated to this cleanup — backing/NORD metadata drifted between key variants, not yet fixed). What happened to each orphan:
  - `Precious Love` (Bob Welch) and `Drive` (The Cars) — clean, already-conformant headers; imported via `./import-song` with no edits needed.
  - `JustWhatINeeded.cho` (The Cars) — turned out to be a genuinely empty (0-byte) placeholder from the 2026-06-01 "80s" batch commit. **Deleted** per owner decision (no content existed to import).
  - `FireOnTheMoutain.cho` (Marshall Tucker Band) — a raw internet-download leftover from the very first `.cho` import (2025-06-20, source comment `# From: jk13@aol.com`), never migrated to house style; two later cleanup attempts (2026-03-01) explicitly failed to fix it. Root cause: a stray non-directive attribution line (`# From: ...`) sat *before* `{title:}`, and `SongParser` aborts header parsing on the first unrecognized line — so `{t:}`/`{st:}` never even got evaluated. Fixed by removing the stray line and converting `{t:}/{st:}` → full `{title:}/{artist:}` + added `{key: Em}` (inferred from the Em/C/G/D/Am chords used) + preserved the songwriter credit as `{comment: Written by George McCorkle}`. TEMPO/DURATION left blank (no source data) — fill in via `song-catalog.csv` once someone times it. **Filename typo `FireOnTheMoutain` (missing an 'n') left as-is** per owner's call — not renamed.
  - **Gotcha for future agents:** don't run multiple `import-song` calls in the same parallel tool batch — it's a plain read-modify-write CSV append with no locking, so concurrent writers clobber each other silently (no error, no exit code, just a vanished row). Run them sequentially, one at a time.

- **Transposer hardened, Phase 0 of key-aware-transposition-and-drift-verification.md (session 19)**: `ChordProTransposer` converted from a static utility (with a stray `main()` dev-test method, now removed) to a proper `@Service`, no callers yet (still not wired to a CLI command — that's Phase 1). Three real fixes: (1) **slash-chord bass notes now transpose** — `[A/E]` up 2 correctly produces `[B/F#]`, previously only the root moved (the exact gap `consistent-song-data.md` §9.1 flagged in June); (2) **non-chord-bracket guard** — a new `QUALITY_PATTERN` validates the text between root and bass/closing-bracket against real chord-suffix grammar (`m`, `maj7`, `sus4`, `add9`, `dim7`, `m7b5`, trailing `*` mute markers, etc., derived from grepping actual usage across the catalog) before treating a bracket as a chord at all — `[Bridge]`, `[Chorus]`, `[Bass]`, riff notation (`[E F# G A]`), and guitar-tab fragments now pass through untouched instead of getting their first letter mangled; (3) **key-driven spelling** — new overload `transpose(line, halfSteps, MusicalKey targetKey)` derives flat-vs-sharp automatically from a new `MusicalKey.prefersFlats()` method (circle-of-fifths based), so callers no longer have to pass `useFlats` by hand. `MusicalKey.prefersFlats()` documents two deliberate tie-breaks for genuinely-ambiguous enharmonic keys: major position 6 defaults to sharp (F# over Gb — more common in this band's repertoire) and minor position 3 defaults to flat (Ebm over D#m — D# minor is essentially never used in practice). New `ChordProTransposerTest` (19 tests, previously zero tests existed for this class) plus 8 new `MusicalKeyTest` cases for `prefersFlats()`. Known, documented limitation: `[C6/9]`-style chords (extension containing a literal `/` that isn't a bass note) are still left untransposed — same tradeoff the original spec's regex made, to avoid false-positives elsewhere. Full design doc: `key-aware-transposition-and-drift-verification.md`. Next: Phase 1 (`transpose` CLI command).
- **Data-cleanup sidebar (session 19, before Phase 1)**: While auditing real bracket content for the Phase 0 quality grammar, found and fixed genuine bad data (as opposed to legitimate notation like fret-position hints/riffs, which were confirmed and left alone): `[Gmjaj7]` typo in `Eagles/ICantTellYouWhy.cho` → `[Gmaj7]`; `[C67]` typo (×8 total across `ScottDavidson/DiminishedIdea.cho`, `WhenWillWeSingTogetherAgain.cho`, `DreamDrift.cho`) → `[C6]` — confirmed as a consistent typo across all 3 of Scott's own songs before fixing, not guessed from one file; and `MuddyWaters/Caldonia.cho` had 4 lines of a broken, incomplete 6-string guitar-tab diagram (copy-pasted with lines concatenated wrong) where 2 lines accidentally looked like chords (`[B|...`, `[D|...`) — deleted, added no value and was already broken. Confirmed-legitimate/untouched: `RunningOnEmpty-g.cho` fret-position hints, `AintNobody-Guitar.cho` riff notation, `AHorseWithNoName.cho`'s real `[D6/9 /F#]` chord (still an untransposable known gap), `DiminishedIdea.cho`'s `[Gdim79]` (intentional, already parses fine). Regression guardrail decided: rather than a separate lint tool, Phase 1's `transpose` command will WARN (not fail) on any bracket that looks like a chord attempt but fails the quality grammar — catches this class of bug at the point it'd actually matter, no duplicated logic. Full details: `key-aware-transposition-and-drift-verification.md` §12.
- **Phase 1 shipped: `transpose` CLI command (session 20)**: `./transpose <input.cho> --offset N --output <path>` — rewrites `{key:}` and every chord in the body, spelled correctly for the target key; `--output` is mandatory and the service hard-errors if it resolves to the same file as the input (`TransposeUseCase`/`TransposeService`/`TransposeCommand`, same hexagonal triangle as every other command). `MusicalKey` gained `noteName()` (now the single source of truth for chromatic spelling — `ChordProTransposer`'s old private sharp/flat arrays were deleted in favor of it), `canonicalName()` (renders `"F#"`/`"Bbm"` for the header), and `transposeBy()` (preserves major/minor quality). **Guardrail bug caught before shipping**: a naive first cut of the promised "warn on unrecognized chord attempts" guardrail reused the same "fails `isValidQuality`" check that safely means "leave untouched" during transpose — but that would also warn on every `[Bridge]`/`[Chorus]` in the catalog, since that's the exact same condition. Fixed with a separate, narrower `isKnownNonChordAnnotation` check grounded in a real grep audit: known section labels (`bridge`/`chorus`/`bass` — the only 3 that actually appear, exact match), riff notation (structural: 2+ space-separated note tokens), and fret-position hints (structural: quality ends in a whitespace+parenthetical with a valid-or-empty prefix — also documents why fret-hinted chords still don't actually get transposed, a pre-existing Phase 0 gap). Confirmed live against a synthetic bad file: `[Gmjaj7]`/`[Fmjaj7]` warned, `[Bridge]` on the same line stayed silent. Live end-to-end tested against real catalog files too, not just unit tests: `HollywoodNights.cho` +5 semitones (E→A) with slash chords correct (`A/E→D/A`, `D/E→G/A`), line count unchanged; `RunningOnEmpty-g.cho` (fret-hint-heavy) produced zero false-positive warnings. 145/145 tests green, `./build` succeeds. **Two things found and flagged, not fixed (out of scope for this phase):** (1) a handful of single-word bracket typos/accidental-lyric-brackets surfaced by the same grep audit — `[Barely]`, `[Come]`, `[down]`, `[drop]`, `[Everywhere]`, `[Fly]`, `[for]`, `[Get]`, `[Ahhhhh]`/`[Ahhhhhh]`, `[Capo:]`, `[Chord]`, `[DEsus]` — the new warning guardrail will surface these naturally the first time someone transposes those specific songs; (2) a cross-cutting, pre-existing bug affecting **every** command in the app: `ChordproParserApplication.run()` discards `CommandLine.execute()`'s return code, so the process always exits `0` even when a command throws (confirmed live on the missing-file and same-input-output guards — correct error printed, exit code still 0). Full details: `key-aware-transposition-and-drift-verification.md` §6.1. Next: Phase 2 (`verify-sync`).
- **Data cleanup: non-chord square brackets converted to parens (session 21)**: acting on the 12 sketchy brackets Phase 1 surfaced, Scott called it — square brackets are now reserved for real chord designators only, going forward. Fixed 9 clear/unambiguous ones (`[Capo:]no capo` → `(Capo: no capo)`, `[Barely]`→`(Barely)`, `[Ahhhhh]`/`[Ahhhhhh]`, `[Fly]`, `[Come]`, `[Get]` x2, and `[Stop-No][Chord]` combined into one `(Stop - No Chord)`). Left 4 unfixed, flagged for Scott's sign-off rather than guessed: `NothinOnYou.cho`'s `[Everywhere]`/`[I]` look like leftover scrape garbage (stripping them entirely yields a clean lyric); `LongTrainRunning-MVP.cho` lines 11 & 13 are badly shattered — a multi-word chord-technique annotation got split one-word-per-bracket by some prior bad edit, interleaved with real lyrics (concatenating the non-bracket text alone reconstructs perfectly clean lyrics + coherent technique notes like "add 7th then drop 6th", but that's a bigger rewrite than a bracket swap); `AndWeDanced.cho`'s `[DEsus]` is ambiguous (real two-chord split vs genuine one-off annotation). 145/145 tests still green (data-only, no code touched).
- **Phase 2 shipped: `verify-sync <fileA> <fileB>` CLI command (session 21)**: catalog-agnostic drift engine, no `SongId` knowledge needed — `VerifySyncUseCase`/`SemanticDiffService`/`VerifySyncCommand`, same hexagonal triangle, report+exit-code convention copied from `consistent-metadata` (exit code = number of findings, 0 = clean). Two independent checks per body line: **lyric drift** (strip `{...}`/`[...]`, normalize whitespace, compare line-by-line) and **harmonic drift** (each chord's root → Roman-numeral scale degree relative to *that file's own* `{key:}`, compare degree sequences — this is what makes pure transposition never false-positive). New `MusicalKey.romanNumeralDegree(int)`: the 7 diatonic degrees match the spec exactly; the 5 chromatic positions per mode are a documented convention (major = "flat of the degree above", matching standard rock/pop borrowed-chord names like bVII; minor = "sharp of the degree below", matching how harmonic/melodic minor actually raise the 6th/7th — position 11 as `vii°` is the real harmonic-minor leading-tone chord, not a guess). New `ChordProTransposer.extractChordRoots()` reuses the existing chord regex — no second grammar to keep in sync. Live end-to-end tested, not just unit tests: the real `HollywoodNights.cho`/`HollywoodNights-b.cho` key-variant pair reported clean; `transpose`'s own +3-semitone output vs the original reported clean; a deliberately corrupted copy (one lyric typo + one chord substitution `[A/E]`→`[D/A]`) correctly caught both a LYRIC and a HARMONIC finding (`IV (A)` vs `bVII (D)`), exit code 2. 162/162 tests green, `./build` succeeds. Full details: `key-aware-transposition-and-drift-verification.md` §7.5. Next: Phase 3 (`consistent-song-data`).
- **Phase 3 shipped: `consistent-song-data <songId>` CLI command (session 22) — the whole design is DONE, remaining items deferred per §9**: thin catalog-aware wrapper around Phase 2's engine, no second diff implementation — `ConsistentSongDataUseCase`/`ConsistentSongDataService`/`ConsistentSongDataCommand`. Resolves every key-variant for a song's group via `CatalogPort`/`SongId.toGroupKey()` (same pattern `UpdateSongService` uses), picks the base variant as reference (falls back to the first entry for an orphan group with no base, same fallback `ConsistentMetadataService` uses), then calls `verify-sync`'s engine once per other variant and aggregates the exit code across all of them. One deliberate small deviation from the design doc's architecture diagram: depends on the `VerifySyncUseCase` **port** rather than the concrete `SemanticDiffService` class — same engine either way, just a stricter hex-architecture choice, and it made the service trivially testable by mocking the port directly instead of its transitive dependencies. `check(SongId)` accepts *any* variant's ID and resolves the whole group regardless — asking about `HollywoodNights-b` gives the identical result as asking about the base `HollywoodNights`. Live end-to-end tested against the real catalog, not just unit tests: real `HollywoodNights` (base+`-b`) reported clean; a song with no key-variants (`RocketMan`) reported "nothing to check" cleanly (not an error); a bogus song ID threw the expected clear error; a deliberately-corrupted temp copy of `HollywoodNights-b.cho` was caught correctly (1 lyric issue, exit code 1) then restored, confirmed via `git status` the real catalog file was untouched. 169/169 tests green, `./build` succeeds. **This closes out the whole `key-aware-transposition-and-drift-verification.md` design** — everything through Phase 3 is shipped; remaining items (`--fix` mode, capo directive, version-specific annotations, `--to-key` convenience flag, full chord-extension-aware Roman numerals, and the still-open cross-cutting exit-code-always-0 bug from Phase 1) are explicitly deferred per §9 of the design doc, not forgotten. Full details: `key-aware-transposition-and-drift-verification.md` §8.1.

### Stubbed / not yet implemented
- **`verify-hardware` (DESIGN ONLY, session 18)**: Spec'd in `docs/architecture/audio-workflow.md`. Would generate/consume a `manifest.json` of SHA-256 hashes per RC-500 slot (Track 1 = click/timing, Track 2 = backing/rhythm), stream bytes directly off the mounted looper over USB (no local copy), and flag MATCH / MISMATCH / MISSING / ORPHAN per slot before a gig. Depends on the existing `Rc500MemoryBank` infra + `deploy-rc500`'s slot-assignment model. **No code exists yet — doc is a thought-experiment / plan.**
- **`stabilize-song-body-delimiter` (DESIGN ONLY, session 16)**: Plan doc `stabilize-song-body-delimiter.md` at repo root. Goal: insert an explicit non-displaying delimiter line into every `.cho` to unambiguously separate header region from song body (today `SongParser` *infers* the boundary as the first non-header line — fragile, caused a duplicate-metadata incident on `UnderTheMilkyWay.cho`). Prerequisite/foundation for `consistent-song-data.md`. **Not built yet — plan only.**
- **`consistent-song-data` (DESIGN ONLY)**: Plan doc `consistent-song-data.md` at repo root. Would diff song *bodies* (chords/lyrics/structure) across key-variants — the body-level counterpart to the metadata-level `consistent-metadata` command. Depends on the body-delimiter work above. **Not built yet.**
- **RC-500 `.RC0` command**: `Rc500MemoryBank` infrastructure fully modelled but not wired to any picocli command. (`deploy-rc500` handles audio file deployment via generated scripts; the `.RC0` command would handle memory-bank name/config updates — a separate future capability.)
- **ChordProTransposer**: Implemented but not wired to any command.

### Ongoing / manual
- **copySetlist**: Still maintained manually per gig. Goal: eventually drive entirely from `gigs.csv` via `export-setlist`.
- **SET leaving `.cho` files**: SET directives were removed from `.cho` files in Phase 3. `HeaderDirective.SET` constant can be removed in a future cleanup pass.

---

## WORKFLOW (normal session)

**Adding a new song:**
1. Drop `.cho` file into `cho/CLUSTER/LETTER/Artist/` directory
2. `./import-song cho/...` (add to catalog; use `--dry-run` to preview first)
3. Fill in metadata in `song-catalog.csv` in Google Sheets
4. Save CSV → `./tidy-song-catalog` → `./update-song <path>` to push metadata into file
5. `./verify-catalog` to confirm everything is consistent

**Updating song metadata:**
1. Edit `song-catalog.csv` in Google Sheets or Excel
2. Save CSV → run `./tidy-song-catalog` to strip \r
3. `./update-song` or `./update-songs` to push metadata into .cho files
4. `./verify-catalog` to confirm
5. `./consistent-metadata` to confirm key-variants of the same song didn't drift apart (run with `--fix` to auto-repair DRIFT; filename/key mismatches must be fixed by hand)

**Planning / running a gig:**
1. `./list-gigs` to see existing gig slugs
2. `./copy-gig <prior-gig> <new-gig>` to clone a prior setlist as a starting point
3. Edit `gigs.csv` in Sheets — adjust SET codes, swap SONG IDs
4. Save CSV → `./tidy-gigs` → `./export-setlist --gig <slug>` to preview the fan setlist (no Z-sets)
5. `./export-setlist --gig <slug> --verbose` to see the full list including backup songs
6. **After setlist order is locked:** `./assign-backing-track-slots --gig <slug>` — assigns RC-500 slots, updates `gigs.csv` + `.cho` files, regenerates `setlist.csv`
7. **After slots assigned:** `./deploy-rc500 [--gig <slug>]` generates `deploy-rc500-<timestamp>.sh`; review/trim the script, then run it against the RC-500
8. `./copyChoSetlist` or `./copySetlist` to stage files for OnSong

**Finding a SONG ID to add to a setlist:**
```zsh
./find-song-id "piano"   # → one row per song, SONG ID column safe to paste into gigs.csv
```

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

- `import-song` replaced `generate-song-catalog` in session 12. The old command would batch-regenerate the entire catalog from `.cho` files, clearing any catalog-only fields (PERFORMANCE KEY etc.) that weren’t in the `.cho` headers — a footgun. `import-song` is idempotent: it adds one song at a time and never touches existing rows.
- **DATA MODEL DECISION (session 7):** Full analysis in `docs/architecture/data-storage-options.md`. Summary:
  - CSVs ARE the database. No binary DB in Git (kills diffs + Google Sheets editability).
  - Split into `song-catalog.csv` (song identity + hardware) and `gigs.csv` (gig planning). Join on SONG ID at runtime in Java. **PHASE 1 COMPLETE (session 8); fully renamed/refactored session 12.**
  - `gigs.csv` columns: `GIG, SONG ID, SET, RC SLOT` (4 cols). **SONG ID must be a base version** (session 11 invariant, enforced by mapper). RC SLOT is blank until `assign-backing-track-slots` runs for that gig.
  - Seeded with 2 placeholder rows (gig=`tbd`). User to recreate real gig data from past setlists.
  - New infrastructure: `SetlistAssignment` (domain), `SetlistAssignmentDto`, `SetlistAssignmentMapper`, `SetlistAssignmentsPort` (out), `SetlistAssignmentsFileReader`, `SetlistAssignmentsFileWriter`, `SetlistAssignmentsAdapter`, `ChordproGigsPathConfig`.
  - Property: `chordprotools.gigs=./gigs.csv` (renamed from `chordprotools.setlist-assignments` in session 12)
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
  - **Option B — base-version-only setlist FK (session 11):** `SetlistAssignmentMapper.toEntity()` rejects variant SONG IDs with `IllegalArgumentException`. `SetlistJoiner.buildEntry()` throws `IllegalStateException` on missing catalog entry. `SetlistDeduplicator` simplified to single-pass duplicate guard. `setlist-assignments.csv` data-migrated (1 row fixed). Tests: 70 passing.
  - SET should eventually leave HeaderDirective + .cho files entirely (gig data ≠ musical data).
  - H2/SQLite rejected: binary formats, non-editable in Sheets, no capability gain at 500-row scale.
  - Google Sheets stays as the editing surface — hard constraint, guitarist must be able to edit without tooling.
- Catalog key is SongId string (e.g. `ABC:B:BillyJoel:MyLife`), NOT file path
- `tidy-song-catalog` / `tidy-gigs` MUST be run after any spreadsheet save before update-song/update-songs or gig commands
- Key variant files (`Song-c.cho`) matched by regex `-[a-gA-G][#b]?m?` — non-key suffixes like `-old`, `-MVP` are NOT variants
- `BACKING` value is a device type (`RC` or `BB`); blank = no backing. Sentinel `99` is deprecated and no longer used.
- `COUNTIN` value `24` = default (no count-in)
- `SONG LABEL` max 12 chars (RC-500 hardware limit) — mapper logs WARN if exceeded, does NOT truncate
- `NORD` / `VE` null value sentinel = `"null"` string
- Java version in pom: 21. README now correctly states 21 (was 17 before session 11 rewrite).
