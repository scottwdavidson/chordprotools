# Data Storage — Options, Analysis & Recommendation

**Context:** Pour Choices band tool. ~500 songs, 2 non-technical users (one of whom
edits the catalog directly in Google Sheets). No database server. Files stored in Git.

---

## 1. The Actual Problem(s)

Before evaluating storage options it's worth being precise about what is painful today
and what might become painful as set management matures.

### 1a. The generate-song-catalog data-loss trap

`generate-song-catalog` reads every `.cho` file and rebuilds the entire catalog from
scratch. Any column value that has been edited in Google Sheets **but not yet pushed
back to the `.cho` files via `update-songs`** is silently overwritten with blank.

The affected columns are: `SET`, `PERFORMANCE KEY`, `SONG LABEL`, `BACKING`, `NORD`,
`ROLAND`, `VE`, `COUNTIN`.

This is documented as a gotcha ("always push edits first") but it is a latent foot-gun.
The tool cannot enforce the ordering; it relies entirely on discipline.

**Root cause:** the catalog has two classes of data that are conflated in one file:

| Class | Origin | Example columns |
|---|---|---|
| **Song identity** | Derived from `.cho` file headers at parse time | TITLE, ARTIST, KEY, TEMPO, DURATION |
| **Gig metadata** | Edited directly by humans, optionally pushed to `.cho` | SET, PERFORMANCE KEY, SONG LABEL, BACKING, NORD, VE |

The generator knows only the first class. The second class needs to survive a
regeneration safely.

### 1b. Growing set complexity

A single `SET` column is fine for one setlist at a time. As the band builds up a
library of gigs ("the A-set we played at the Rusty Nail in June", "the theatre gig",
"the fundraiser"), a single column becomes inadequate. You'd want: gig name / date,
set letter (A/B/C), position within set, maybe gig-specific notes.

### 1c. The Google Sheets dependency

The guitarist edits the catalog in Google Sheets. This is a hard UX constraint. Any
storage format that can't be opened in Google Sheets (or a spreadsheet equivalent)
without additional tooling is effectively off the table for the **gig metadata** class
of data.

---

## 2. Constraints Enumerated

1. **No persistent database server.** Nothing that requires `start-server` before
   running the tool.
2. **Guitarist-editable.** Gig metadata must be editable in Google Sheets or equivalent
   with zero friction.
3. **Git-storable.** All data lives in the repo. Diffs must be human-readable — merge
   conflicts must be resolvable without specialist tools.
4. **Single-process.** No concurrent writes; one person edits at a time. ACID and
   concurrent-access guarantees are not needed.
5. **Sub-500 songs.** Scale is a non-issue. This will never be a query-performance
   problem.
6. **Spring Boot already running** when any command executes — in-memory processing
   is essentially free.

---

## 3. Options

### Option 0 — Fix generate-song-catalog (merge-preserve)

**What:** Change `generate-song-catalog` so that before rebuilding the catalog it loads
the *existing* catalog into memory, builds a map of `SONG ID → {gig-metadata columns}`,
and after parsing all `.cho` files, merges those preserved values back onto any matching
entry. New songs get blank gig-metadata as today; existing songs keep whatever was in
the catalog.

**Effort:** ~30 lines of Java in `GenerateSongCatalogService`. No structural change.

**What it solves:** eliminates the data-loss trap entirely. The workflow ordering
constraint (`update-songs` before `generate-song-catalog`) becomes irrelevant.

**What it does not solve:** the growing set complexity problem (1b). A single `SET`
column can only describe one active setlist at a time.

**Verdict:** ✅ Should be done regardless of which other option is chosen. It's cheap
insurance and closes the biggest current foot-gun.

---

### Option 1 — Two CSVs: song-catalog + setlist-assignments (recommended)

**What:** Split the single CSV into two files along the class boundary identified in §1a.

```
song-catalog.csv          ← song identity + hardware presets
                            (TITLE, ARTIST, KEY, TEMPO, DURATION,
                             BACKING, NORD, ROLAND, VE, COUNTIN,
                             SONG LABEL, PERFORMANCE KEY, CAPO,
                             TIME SIGNATURE, SONG ID)

setlist-assignments.csv   ← gig metadata only
                            (SONG ID, GIG, SET, POSITION, NOTES)
```

`song-catalog.csv` is regenerated freely by `generate-song-catalog` — it contains only
data that can be reconstructed from `.cho` files. No merge-preserve needed, no gotcha.

`setlist-assignments.csv` is pure human input. The tool never overwrites it. Google
Sheets opens it alongside the song catalog (two tabs) when planning a setlist.

Commands that need both (e.g. `export-setlist`, `assign-backing-track-slots`) join on
`SONG ID` in memory — a trivially simple join in Java streams.

**Supporting multiple setlists:** the `GIG` column (e.g. `"2026-06-14 RustyNail"`,
`"theatre-2026-09"`) means you can have as many historical and future setlists as you
want in one file. Filter by GIG to get the setlist for any gig. The `SET` column
becomes `A`, `B`, `C` (not a compound code like `A01` — position is now its own column).

**Git diffs:** changes to setlists never touch song-catalog.csv and vice versa. PR
reviews and `git log` become much more meaningful. Merge conflicts are isolated to the
file that actually changed.

**Effort:** moderate. Requires:
- New `SetlistAssignment` domain model
- New `SetlistAssignmentsPort` (read/write the new CSV)
- Update `export-setlist` / `assign-backing-track-slots` to join the two sources
- Migration script: split current `SET` column out of `song-catalog.csv`
- Remove `SET` from `CatalogEntry`, `HeaderDirective`, and all `.cho` round-trip logic
  (SET becoming a pure catalog concept breaks the current `.cho` round-trip for that
   field — but that's arguably correct: the `.cho` file shouldn't know what set it's in)

**Verdict:** ✅ Best long-term architecture. Solves both problems cleanly. Keeps Google
Sheets as the editing surface. Keeps Git diffs readable.

---

### Option 2 — SQLite file in Git

**What:** Replace both CSVs with a SQLite `.db` file committed to the repo. Spring Boot
loads it at startup via JDBC. No server needed — SQLite is an in-process library.

**The Git problem:** SQLite files are binary. `git diff` produces garbage. Merge
conflicts are resolved as "take ours" or "take theirs" — there is no line-level merge.
Two band members making independent edits is a recipe for silent data loss on merge.

**The Google Sheets problem:** Google Sheets cannot open a SQLite file. The guitarist
would need to install DB Browser for SQLite, learn to use it, and remember to commit
the changed `.db` file after every edit. This violates constraint 2 completely.

**What it gains:** proper relational structure, foreign key enforcement, real SQL
queries. None of these are meaningful at 500-row scale with single-user access.

**Verdict:** ❌ The binary-in-Git and non-editable-in-Sheets problems are both
disqualifying. SQLite is the right tool for many problems; this isn't one of them.

---

### Option 3 — H2 in-memory, CSV as source of truth

**What:** Load the CSV(s) into an embedded H2 database at Spring Boot startup, run all
operations via SQL/JPQL, write back to CSV on exit or after mutations.

**What it gains over Option 1:** SQL joins and aggregations instead of stream pipelines.
Marginally easier to add complex queries later.

**What it costs:** significant complexity for zero user-visible benefit. The CSV is
still the source of truth. The database is a runtime implementation detail. The
write-back step introduces a failure mode (crash between mutation and write-back =
data loss). H2 adds a dependency and startup overhead.

**Honest assessment:** this is Option 1 with extra steps. The "database" only exists in
the JVM heap for the duration of one CLI command execution. Java streams over a
500-row list are fast enough that this adds no meaningful capability.

**Verdict:** ❌ Over-engineered for the problem size. Option 1 gives you the same data
model with less moving parts.

---

### Option 4 — H2 file-based (`.h2.mv.db` in Git)

Like Option 2 (SQLite) but with H2's proprietary binary format. Same disqualifying
problems: binary in Git, uneditable in Sheets. Actually worse than SQLite because the
format is less portable and tooling is H2-specific.

**Verdict:** ❌ Strictly worse than SQLite for this use case.

---

### Option 5 — Google Sheets as primary store, CSV as export

**What:** The "database" IS Google Sheets. The tool reads from a published CSV URL or
the Sheets API. Edits happen in Sheets directly. `generate-song-catalog` and friends
pull from Sheets rather than local files.

**What it gains:** no file management, always-current view for all band members, real
collaborative editing, built-in version history.

**What it costs:**
- Requires internet connection to run any command (currently fully offline)
- Sheets API auth is non-trivial to set up and maintain credentials for
- The repo no longer contains the authoritative data — Git history becomes incomplete
- `tidy-song-catalog` (strip `\r`) becomes the Sheets export step instead

**Verdict:** ⚠️ Interesting if the band ever needs true multi-user concurrent editing.
Not worth the complexity for current usage patterns. Revisit if the band grows.

---

## 4. The "Database in Git" Question Directly

You asked specifically about in-memory DB stored in Git. This framing reveals the core
tension:

> A database's value is its *engine* (query planner, transactions, constraints).
> Storing the DB *file* in Git wants the engine's format to also be *diff-friendly*.
> These two requirements are in tension for every binary DB format.

The way to resolve this tension is to recognize that **the CSV IS the database file**.
It just uses a simpler engine (the JVM + streams). At 500 rows with single-user access,
the "engine" capability gap between CSV+streams and SQLite+JDBC is zero for every
operation this tool will ever perform.

The thing that CSV lacks relative to a relational DB is not query capability — it's
**schema enforcement and relationship modeling**. Option 1 (two CSVs) addresses the
relationship modeling. Schema enforcement at 500 rows is handled by the Java type
system and the mapper layer.

**Conclusion:** the DB-in-Git concept is solved most pragmatically by treating
well-structured CSVs as relational tables and performing joins in the application layer.
This is exactly what Option 1 does.

---

## 5. Recommendation Summary

| Priority | Action | Effort | Value |
|---|---|---|---|
| **Now** | Implement Option 0: merge-preserve in generate-song-catalog | Small | Eliminates data-loss gotcha immediately |
| **Next** | Implement Option 1: split song-catalog + setlist-assignments | Medium | Correct long-term model; enables multiple gig tracking |
| **Later** | Consider Option 5 (Sheets API) | Large | Only if multi-user concurrent editing becomes needed |
| **Never** | Options 2, 3, 4 (any binary DB in Git) | Any | Wrong tool for the constraints |

### What Option 1 looks like for the guitarist

Working on a setlist for the June gig:
1. Open `setlist-assignments.csv` in Google Sheets
2. Add/edit rows: `2026-06-14 Rusty Nail | SongId | A | 3 | play slow intro`
3. Save → `./tidy-song-catalog`
4. `./export-setlist --gig "2026-06-14 Rusty Nail"`

No DB to start. No tooling to install. Same Google Sheets experience as today,
except set changes can never accidentally destroy song metadata.

---

## 6. One Thing Worth Noting About SET in `.cho` Files

Currently `SET` is written into `.cho` files as `{meta: Set: A01}`. This means the
`.cho` file "knows" what set it belongs to — which is arguably wrong. A song's musical
content (chords, lyrics, key, tempo) is stable. Its set assignment is ephemeral gig
planning data. Mixing these concerns in the same file means every set change requires
touching `.cho` files, which are the band's core musical assets.

Under Option 1, `SET` leaves `HeaderDirective` entirely and lives only in
`setlist-assignments.csv`. The `.cho` files become purely musical documents. This is
a cleaner separation and reduces the blast radius of `update-songs` runs.

---

*Document generated: 2026-05-15 | Author: Kino (code-puppy-90bb29)*
