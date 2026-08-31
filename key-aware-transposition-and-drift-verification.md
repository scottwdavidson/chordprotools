# Design: Key-Aware ChordPro Transposition & Semantic Drift Verification

> **Status:** Phase 3 complete (2026-08-30) · Phase 4 (SONG-ID-driven
> `transpose` orchestration) complete (2026-08-31), see §13 · remaining work
> is deferred (§9) · **Author:** Kino  (absorbing a spec drafted with
> another agent) · **Date:** 2026-08-29
> **Scope:** A `transpose` command that key-spells correctly, plus a generic
> `verify-sync` drift engine reused by the already-parked `consistent-song-data`
> design.
> **Siblings:** `consistent-metadata.md` (catalog metadata — shipped),
> `consistent-song-data.md` (body/content drift — parked design, Phase 3 here
> absorbs and layers on top of it instead of duplicating it).

---

## 1. Problem statement

Two needs, originally scoped together:

1. **Transposition** — given a `.cho` file with a `{key: X}` directive, produce
   a correctly transposed copy in a target key, with chords re-spelled using
   the *correct* accidentals for that key (flat keys get flat spellings, sharp
   keys get sharp spellings), including chords with a slash bass note
   (`[A/E]`).
2. **Drift verification** — given two files that are supposed to be the same
   song in different keys (or the same key), detect when they've actually
   diverged: different lyrics, different chord progression, or a structural
   difference — as opposed to just being a transposition of each other (which
   is fine).

This doc was seeded from a spec drafted in a separate session that didn't have
visibility into this repo's existing code. Before writing anything, the
codebase was audited and it turns out most of the *hard part* already exists
in embryonic form — see §2. This changes the plan from "build from scratch"
to "harden and extend."

---

## 2. What already exists (audit findings)

| Component | File | Status |
|---|---|---|
| Root-note transposition, sharp/flat scale tables, enharmonic normalization (`E#→F` etc.), double-accidental handling | `application/domain/service/ChordProTransposer.java` | **Built, not wired to any command.** Static utility class with a stray `main()` dev-test method. |
| Key model: chromatic position (0–11) + major/minor, correct enharmonic equality (`A#` ≡ `Bb`) | `application/domain/model/MusicalKey.java` | **Built and tested** (`MusicalKeyTest.java`). Does **not** yet know flat-vs-sharp *spelling preference* — callers pass `useFlats` manually today. |
| Header/body parsing, `{key:}` directive access | `SongParser`, `ParsedHeader`, `ParsedHeaderLine`, `HeaderDirective.KEY` | **Built and used everywhere.** No changes needed to consume. |
| File I/O | `application/port/out/ChordProPort` (`read`/`write`) | **Built.** *(Note: this whole `port/out/` directory doesn't show up in some directory-listing tools — worth remembering if a future session says "there's no I/O port," it's a tooling blind spot, not reality.)* |
| Body/lyric-vs-chord drift design | `consistent-song-data.md` (parked, June 2026) | **Design only, never built.** Already documents the exact slash-chord gap below and proposes `SongBodyCanonicaliser`/`SongBodyDiffer`, scoped to catalog `SongId` groups. |
| `HeaderDirective.CAPO` | — | **Does not exist.** No `.cho` file in the catalog currently uses a capo directive. |

### Two real bugs inherited by any transposition feature

1. **Slash chords transpose incorrectly.** `ChordProTransposer`'s regex
   (`\[([A-G][#b]*)`) only matches the chord *before* a `/`, so `[A/E]`
   becomes `[C/E]` when transposed up 2 — the bass note never moves. Flagged
   already in `consistent-song-data.md` §9.1 as a hard blocker for any
   `--fix`-style feature.
2. **Non-chord brackets get mangled.** The same regex greedily matches any
   bracket starting with a letter A–G, so section labels like `[Bridge]`,
   `[Chorus]`, `[Bass]` get their first letter "transposed" into garbage.
   Never caught before because nothing has run real transposition yet.

Both must be fixed before *anything* built on top of the transposer (the
`transpose` command **and** the harmonic-drift check) can be trusted. This is
why Phase 0 exists as its own phase, before any CLI command ships.

---

## 3. Resolved decisions

| Question | Decision |
|---|---|
| Rewrite per the new spec's `PitchClass`/`KeySignature`/`ChordToken` model, or extend what exists? | **Extend existing classes.** `MusicalKey` and `ChordProTransposer` are tested and correct in scope; rewriting would throw away working, tested chromatic math for no functional gain. Add an accidental-preference capability to `MusicalKey`; harden `ChordProTransposer` in place. |
| How does `verify-sync <fileA> <fileB>` relate to the parked `consistent-song-data <songId>` design? | **Layered, not duplicated.** `verify-sync` becomes the generic, catalog-agnostic diff *engine* (two raw file paths). `consistent-song-data` (Phase 3, later) becomes a thin catalog-aware wrapper that resolves a song group's variant files and calls the same engine. One diff implementation, two entry points. |
| Build order? | **Phase 0 (harden transposer) always comes first**, regardless of which CLI command ships next — both `transpose` and the harmonic-drift half of `verify-sync` depend on correct chord parsing. |
| Can `transpose` ever overwrite the input file? | **No. `--output <path>` is mandatory.** The command errors out if omitted. No silent in-place overwrites, no implicit stdout/file ambiguity. |
| Capo directive handling? | **Skipped entirely (YAGNI).** No `HeaderDirective.CAPO` exists and no cataloged song uses one. Revisit only if a real need shows up. |

---

## 4. Where this fits (hexagonal architecture)

Follows the exact same shape as every shipped command (`consistent-metadata`,
`update-song`, etc.) — no new architectural patterns introduced.

```
Phase 0 (no new command — hardens an existing service in place)
        application/domain/service/ChordProTransposer.java   (extended)
        application/domain/model/MusicalKey.java              (extended)

Phase 1
adapter/in/file/TransposeCommand.java          ← picocli @Command
        ▼
application/port/in/TransposeUseCase.java       ← interface
        ▼
application/domain/service/TransposeService.java
        │  uses SongParser, ChordProTransposer, ChordProPort
        ▼
writes transposed ParsedSong to --output path

Phase 2
adapter/in/file/VerifySyncCommand.java         ← picocli @Command (2 file paths)
        ▼
application/port/in/VerifySyncUseCase.java      ← interface
        ▼
application/domain/service/SemanticDiffService.java
        │  uses SongParser, MusicalKey, ChordProTransposer's chord regex
        ▼
application/domain/model/SemanticDiffReport.java  ← immutable result

Phase 3 (layers on Phase 2 — no new diff logic)
adapter/in/file/ConsistentSongDataCommand.java  ← picocli @Command (songId)
        ▼
application/port/in/ConsistentSongDataUseCase.java
        ▼
application/domain/service/ConsistentSongDataService.java
        │  resolves variant files via CatalogPort/SongId, then calls
        │  SemanticDiffService (same engine as verify-sync)
```

---

## 5. Phase 0 — Harden the transposition engine — **DONE (2026-08-29)**

*No new CLI command. Extends existing, tested classes in place.*

### 5.1 Slash-chord support
Extend the chord regex to capture an optional `/BassNote` group and transpose
it the same as the root:

```
\[([A-G][#b]*)([^/\]]*)(?:/([A-G][#b]*))?\]
```

- Group 1 (root) → transpose as today.
- Group 2 (quality/extensions) → unchanged, preserved verbatim.
- Group 3 (bass, optional) → transpose using the same half-step offset and
  the same target-key spelling as the root.

### 5.2 Non-chord bracket guard
After matching a candidate root note, the remainder up to `/` or `]` must
match a recognized chord-quality grammar (empty, `m`, `maj7`, `m7b5`, `sus2`,
`sus4`, `dim`, `aug`, `add9`, numeric extensions, combinations thereof). If it
doesn't match, the bracket is left untouched — `[Bridge]`, `[Chorus]`,
`[2x]`, `[Instrumental]` all pass through unchanged.

### 5.3 Key-driven accidental spelling
Add a spelling-preference capability to `MusicalKey` based on the circle of
fifths (mirrors the spec's classification exactly):

- **Flat-preference keys:** F, Bb, Eb, Ab, Db, Gb (major) / Dm, Gm, Cm, Fm,
  Bbm, Ebm (minor).
- **Sharp-preference keys:** G, D, A, E, B, F# (major) / Em, Bm, F#m, C#m,
  G#m, D#m (minor).
- **Neutral (default to sharps):** C, Am.

New overload: `ChordProTransposer.transpose(String line, int halfSteps,
MusicalKey targetKey)` — derives `useFlats` internally instead of requiring
the caller to know it. The existing `boolean useFlats` overload stays for
backward compatibility (nothing currently calls it, but no reason to break
the public shape).

### 5.4 Promote to a Spring service
`ChordProTransposer` becomes a proper `@Service` (instance methods, DI-ready)
consistent with the rest of `application/domain/service/*`. The stray
`main()` dev-test method is deleted — its job is now done by real unit tests.
### 5.5 Tests — done

New `ChordProTransposerTest.java` (19 tests — there wasn't one before, a real
gap for something about to become gig-critical):
- Slash chords transpose both root and bass (`[A/E]` up 2 → `[B/F#]`). Done
- Non-chord brackets pass through unchanged (`[Bridge]`, `[Chorus]`, `[Bass]`,
  `[Ending]`, riff notation, guitar-tab fragments, `[2x]`). Done
- Enharmonic normalization still works (`[E#]` → `F`). Done
- Key-driven spelling: transposing into Bb major produces `Eb`, not `D#`;
  transposing into D major produces `F#`, not `Gb`. Done
- Existing behaviors (double accidentals, quality/extension preservation,
  negative half-steps, null/empty input) keep passing. Done
- Malformed real-world typo (`[Fmjaj7]`) is deliberately left untouched
  rather than guessed at. Done
- Documented known gap (`[C6/9]`) has an explicit regression test proving
  it's a deliberate choice, not an oversight. Done
- 8 new `MusicalKeyTest` cases for `prefersFlats()`, including both
  documented enharmonic tie-breaks. Done

Full suite (123 tests, whole project) still green after this change.

---

## 6. Phase 1 — `transpose` command

> **Superseded (2026-08-31, see §13):** the CLI interface described in this
> section was replaced with a SONG-ID-driven one
> (`./transpose <SONG_ID> --offset N [--no-import]`) that also auto-catalogs
> the result. The engine described here (`TransposeUseCase`/`TransposeService`)
> is unchanged and still does the actual transposition — it's just no longer
> exposed directly as its own CLI surface. Kept below for the historical
> record of how the engine itself was built.

```
chordprotools transpose <input.cho> --offset <semitones> --output <path>
```

- `TransposeUseCase` (port/in) / `TransposeService` (domain/service) /
  `TransposeCommand` (adapter/in/file) — same triangle as every other command.
- `TransposeService.transpose(...)`:
  1. Read + parse the input file (`ChordProPort` + `SongParser`).
  2. Extract `{key:}` from the header; error clearly if missing (mandatory
     per spec).
  3. Compute target `MusicalKey` = source key shifted by `--offset` semitones.
  4. Rewrite the `{key:}` header line to the target key's canonical name.
  5. Transpose every body line via the Phase-0-hardened
     `ChordProTransposer`, spelled per the target key.
  6. Write the result to `--output` (mandatory; command errors if absent).
- Shell shim `./transpose` → `./cpt transpose "$@"`, matching every other
  top-level script.
- `TransposeServiceTest`: round-trip correctness, `{key:}` directive updated,
  correct enharmonic spelling for a flat target and a sharp target, slash
  chords carried through correctly, missing-key error case.

### 6.1 Implementation notes (2026-08-30)

- `MusicalKey` gained `noteName(int, boolean)` (static, the single source of
  truth for chromatic note spelling), `canonicalName()` (renders e.g.
  `"F#"`, `"Bbm"` for writing back into `{key:}`), and `transposeBy(int)`
  (preserves major/minor quality). `ChordProTransposer` was refactored to
  call `MusicalKey.noteName()` instead of keeping its own private
  sharp/flat arrays — small DRY cleanup, since both classes work in the same
  12-note chromatic space.
- **Regression guardrail, implemented properly (not naively):** an early
  draft of `findUnrecognizedChordAttempts` just reused "fails
  `isValidQuality`" as the warning trigger — caught by a test failure
  before it shipped, because that also flags every `[Bridge]`/`[Chorus]` in
  the catalog (same root cause that makes `transpose` correctly leave them
  untouched also makes them look "unrecognized"). Fixed with a narrower,
  separate `isKnownNonChordAnnotation` check, grounded in a real grep audit
  of the catalog rather than guessed:
  - **Known section labels** (`bridge`, `chorus`, `bass` — the only three
    that actually appear): allow-listed by exact word match, not warned on.
  - **Riff notation** (`[E F# G A]`): detected structurally (2+
    whitespace-separated valid note tokens), not by a fixed list.
  - **Fret-position hints** (`[C (17th - 3-6)]`, `[Em (9th - 1,3)]`):
    detected structurally (quality ends in a parenthetical preceded by
    whitespace, with a valid-or-empty quality prefix), not by hardcoding
    "17th/9th/etc." This is also why fret-hinted chords still aren't
    actually transposed — same known Phase-0 limitation, just confirmed it
    won't cry wolf about it.
  - Everything else that starts with a note letter but fails
    `isValidQuality` **does** get flagged — confirmed live against a
    synthetic bad file (`[Gmjaj7]`/`[Fmjaj7]` warned, `[Bridge]` on the
    same line stayed silent).
- **Known catalog-quality follow-up surfaced, not yet fixed:** the same grep
  audit turned up several single-word letters-only brackets that don't look
  like intentional annotations and aren't chords either — `[Barely]`,
  `[Come]`, `[down]`, `[drop]`, `[Everywhere]`, `[Fly]`, `[for]`, `[Get]`,
  `[Ahhhhh]`/`[Ahhhhhh]`, `[Capo:]`, `[Chord]`, `[DEsus]`. These read like
  accidentally-bracketed lyric words (or, for `[Capo:]`, a directive that
  should probably be `{capo: ...}` instead of a bracket) but weren't chased
  down as part of this phase — out of scope for "ship `transpose`," and
  exactly the kind of thing the warning guardrail will now surface
  naturally the first time someone transposes those specific songs, rather
  than needing a dedicated sweep today.
- **Exit-code gap found, not fixed (cross-cutting, pre-existing):**
  `ChordproParserApplication.run()` discards `CommandLine.execute()`'s
  return code entirely — `System.exit(SpringApplication.exit(...))` always
  returns `0` regardless of whether the command threw. Confirmed live: both
  the missing-file and same-input-output guards printed a correct error
  and stack trace but still exited `0`. This affects **every** command in
  the app (not new to `transpose`), so it wasn't silently patched here —
  flagged to Scott as a real, separate finding worth its own decision.
- Live-tested end-to-end against real catalog files (not just unit tests):
  `HollywoodNights.cho` transposed +5 (E→A) with slash chords (`A/E→D/A`,
  `D/E→G/A`) all correct and line count unchanged; `RunningOnEmpty-g.cho`
  (fret-hint-heavy) transposed with zero false-positive warnings.

---

## 7. Phase 2 — `verify-sync <fileA> <fileB>` (generic drift engine)

```
chordprotools verify-sync <fileA.cho> <fileB.cho>
```

Deliberately **catalog-agnostic** — takes two raw file paths, no `SongId`
knowledge. This makes it independently testable and reusable by Phase 3.

### 7.1 Lyric drift check
Strip all `{...}` directives and `[...]` chords from both files, normalize
whitespace, compare line-by-line. Differing words or added/removed lines →
**Lyric Desynchronization** finding with line number.

### 7.2 Harmonic drift check
For each file, convert every chord to a scale-degree interval relative to
*that file's own* `{key:}`:

```
interval = (chordRootPitch - fileKeyRootPitch) mod 12
```

Map intervals to Roman numerals using each file's major/minor quality (I, ii,
iii, IV, V, vi, vii° for major; i, ii°, III, iv, v, VI, VII for minor).
Compare the resulting sequences between the two files. A mismatch → report
the line number and both degrees, e.g. `Line 24: File A has V (D), File B has
IV (Ab)`.

> **Known v1 simplification:** Roman-numeral mapping in this phase covers
> triad quality (major/minor/diminished) derived from scale degree, not the
> full chord extension (`maj7`, `sus4`, etc.). Two chords with the same root
> and different extensions on the same degree won't be flagged as harmonic
> drift by this check alone — that's still caught by the lyric/positional
> line compare in most real cases, but it's a known gap worth being explicit
> about rather than silently pretending it's complete.

### 7.3 Report + exit code
Same convention as `consistent-metadata`: human-readable report, exit code =
number of findings (0 = clean, CI-friendly).

### 7.4 Tests
`SemanticDiffServiceTest`:
- Same song, transposed to a different key, otherwise identical → **clean**
  (this is the whole point — pure transposition must never false-positive).
- Genuine lyric change → **Lyric Desynchronization**, correct line number.
- Genuine chord substitution → harmonic drift finding, correct line/degrees.
- Enharmonic spelling difference only (`A#` vs `Bb`) → **clean**, not a false
  drift.

### 7.5 Implementation notes (2026-08-30)

- `MusicalKey` gained `romanNumeralDegree(int)`. The 7 diatonic degrees
  match the spec's exact case/° convention. The 5 remaining chromatic
  positions per mode are a deliberate, documented convention chosen for
  **internally consistent comparison** rather than a claim of being the
  one true music-theory label: major spells chromatics as "flat of the
  degree above" (bII, bIII, bV, bVI, bVII — all standard rock/pop
  borrowed-chord names), minor spells them as "sharp of the degree below"
  (matching how melodic/harmonic minor actually raise the 6th/7th; position
  11 as `vii°` is the real harmonic-minor leading-tone diminished chord,
  not an arbitrary guess).
- `ChordProTransposer` gained `extractChordRoots(String)`, reusing the same
  `CHORD_PATTERN`/`isValidQuality` as `transpose` — no second chord grammar
  to keep in sync.
- Live-tested end-to-end, not just unit tests: `verify-sync` against the
  real `HollywoodNights.cho` / `HollywoodNights-b.cho` key-variant pair
  reported clean; against `transpose`'s own output (offset +3) reported
  clean; against a deliberately-corrupted copy (one lyric typo, one chord
  substitution `[A/E]`→`[D/A]`) correctly reported both a `LYRIC_DESYNC`
  and a `HARMONIC_DRIFT` finding (`IV (A)` vs `bVII (D)`), exit code 2.
- 162/162 tests green, `./build` succeeds.

---

## 8. Phase 3 — `consistent-song-data <songId>` (catalog-aware wrapper)

Resurrects the parked `consistent-song-data.md` design, but **implemented as
a thin wrapper around Phase 2's `SemanticDiffService`** instead of a second
diff implementation:

```
chordprotools consistent-song-data <songId>
```

1. Resolve all key-variant files for the song's group via `CatalogPort` /
   `SongId.toGroupKey()` (same pattern `UpdateSongService` already uses).
2. Pick a reference variant (defaults to the base/standard-key file).
3. For each other variant, call `SemanticDiffService` — same engine, same
   report shape as `verify-sync`.
4. Detection/dry-run only in this phase — matches the original design doc's
   own phased recommendation (harden → detect → fix → annotations).

### 8.1 Implementation notes (2026-08-30)

- One deliberate, small deviation from the architecture diagram in §4:
  `ConsistentSongDataService` depends on the `VerifySyncUseCase` **port**
  rather than the concrete `SemanticDiffService` class. Same engine either
  way (Spring wires whatever implements the port) — just a stricter
  hexagonal-architecture choice, and it made the service trivially
  testable by mocking the port directly instead of its transitive
  dependencies.
- `check(SongId)` accepts *any* variant's ID and always resolves + checks
  the whole group — asking about `HollywoodNights-b` gives the identical
  result as asking about the base `HollywoodNights`.
- Guard-rails: throws if the group has zero catalog entries (bad/typo'd
  song ID); returns an empty report list (not an error) if the group has
  only one variant — nothing to compare isn't a failure.
- Live-tested end-to-end against the real catalog, not just unit tests:
  `HollywoodNights` (base + `-b`) reported clean; a song with no
  key-variants (`RocketMan`) reported "nothing to check"; a bogus song ID
  threw the expected error; a deliberately-corrupted temp copy of
  `HollywoodNights-b.cho` was correctly caught (1 lyric issue, exit code 1)
  and restored afterward, confirmed via `git status` that the real catalog
  file was untouched.
- 169/169 tests green, `./build` succeeds.

---

## 9. Explicitly deferred (not building now)

| Item | Why deferred |
|---|---|
| `--fix` mode for `consistent-song-data` | Mutates `.cho` files; needs the tuning-mismatch refusal logic the original design doc calls out. Real risk, do it as its own reviewed phase later. |
| Capo directive (`HeaderDirective.CAPO`) | YAGNI — nothing in the catalog uses one today. |
| Version-specific annotation convention (`{comment: @only(...)}`) | Original design doc already deferred this; no convention agreed yet. |
| `--to-key <key>` convenience flag for `transpose` | Spec only asked for `--offset`; trivial to add later, not core scope. |
| Full chord-extension-aware Roman numeral mapping | Noted as a known v1 simplification in §7.2; revisit if it causes a missed real-world drift. |

---

## 10. Proposed new/changed files

| File | Type | Phase |
|---|---|---|
| `ChordProTransposer.java` (extend) | service | 0 — slash chords, bracket guard, key-driven spelling, `@Service` |
| `MusicalKey.java` (extend) | model | 0 — accidental-preference method |
| `ChordProTransposerTest.java` | test | 0 |
| `TransposeUseCase.java` | port/in | 1 |
| `TransposeService.java` | service | 1 |
| `TransposeCommand.java` | adapter | 1 |
| `TransposeServiceTest.java` | test | 1 |
| `./transpose` | shell | 1 — shim → `./cpt transpose "$@"` |
| `VerifySyncUseCase.java` | port/in | 2 |
| `SemanticDiffService.java` | service | 2 |
| `SemanticDiffReport.java` (+ nested `Finding`/`FindingType`) | model | 2 |
| `VerifySyncCommand.java` | adapter | 2 |
| `SemanticDiffServiceTest.java` | test | 2 |
| `./verify-sync` | shell | 2 |
| `ConsistentSongDataUseCase.java` | port/in | 3 |
| `ConsistentSongDataService.java` | service | 3 |
| `ConsistentSongDataCommand.java` | adapter | 3 |
| `./consistent-song-data` | shell | 3 |

---

## 11. Why this is low-risk

- Phase 0 touches no CLI surface — it's pure hardening of a service nothing
  currently depends on, backed by new tests that don't exist today.
- Every phase after 0 is read-only or write-to-a-new-path-only (`transpose`
  requires `--output`; `verify-sync`/`consistent-song-data` are pure reports).
- Reuses three already-tested foundations: `MusicalKey`'s enharmonic math,
  `SongParser`'s header/body split, and `ChordProPort`'s file I/O — no new
  I/O or parsing patterns invented.
- The one duplicated-effort risk (building `verify-sync` and
  `consistent-song-data` as two separate diff engines) is explicitly designed
  out in §8 — one engine, two entry points.

---

## 12. Data-cleanup sidebar (2026-08-29, before Phase 1)

While auditing real bracket content for Phase 0's quality grammar, several
genuine data-quality issues turned up (as opposed to legitimate notation the
transposer correctly leaves alone — fret-position hints, lead-guitar riffs).
Cleaned up rather than coded around, per the "fix the data, don't grow the
parser forever" principle:

| File | Issue | Fix |
|---|---|---|
| `Eagles/ICantTellYouWhy.cho` | `[Gmjaj7]` | Typo → `[Gmaj7]` |
| `ScottDavidson/DiminishedIdea.cho` | `[C67]` ×2 | Typo → `[C6]` |
| `ScottDavidson/WhenWillWeSingTogetherAgain.cho` | `[C67]` ×4 | Typo → `[C6]` (same typo, confirmed consistent across files before fixing — don't guess-fix from a single occurrence) |
| `ScottDavidson/DreamDrift.cho` | `[C67]` ×2 | Typo → `[C6]` (kept the adjacent, genuinely distinct `[C69]`/`[C6b9]` chords untouched) |
| `MuddyWaters/Caldonia.cho` | 4 lines of a broken, incomplete 6-string guitar-tab diagram, copy-pasted with lines concatenated wrong, 2 of which accidentally looked like chords (`[B\|...`, `[D\|...`) because their string label happened to be a valid note letter | Deleted — added no value in this lyric+chord format and was already broken |

**Confirmed as legitimate, not bad data (left untouched):**
- Fret-position hints (`[C (17th - 3-6)]`) in `JacksonBrowne/RunningOnEmpty-g.cho`.
- Lead-guitar riff notation (`[A F# E D]`) in `ChakaKhan/AintNobody-Guitar.cho`.
- `[D6/9 /F#]` in `America/AHorseWithNoName.cho` — the song's real, iconic chord;
  still an untransposable known gap (§5.1's slash-in-quality limitation),
  not a data problem.
- `[Gdim79]` in `DiminishedIdea.cho` — confirmed intentional Gdim7(add9);
  already parses correctly under the Phase 0 quality grammar as-is.

---

## 13. Phase 4 - SONG-ID-driven `transpose` orchestration - **DONE (2026-08-31)**

Originally out of scope for this doc (Phases 0-3 above), but raised as a
follow-up once real usage made the gap obvious: the file-in/file-out
interface from Section 6 required the caller to already know and correctly
apply the `-<key>` naming convention by hand, and left the resulting file
un-cataloged. Two manual steps, two chances to get the filename or the
catalog entry wrong.

New interface:

```
./transpose <SONG_ID> --offset <semitones> [--no-import]
```

What changed:

- `SongId.withKeyAlternative(String)` - derives a same-group `SongId` with a
  different (or no) key-alternative suffix, e.g.
  `ABC:B:BillyJoel:PianoMan.withKeyAlternative("bb")` becomes
  `ABC:B:BillyJoel:PianoMan-bb`.
- `ChordProPath.toSongId` hardened to fail with a clear, actionable message
  when handed a path that doesn't start with `cho/`/`./cho/`, instead of
  silently mis-parsing an absolute path into a confusing
  "expected 4 segments but got N" error from deep inside `SongId.parse`.
- `TransposeSongUseCase` (port/in) / `TransposeSongService` (domain/service) -
  composes the existing `TransposeService` (Section 6) and
  `ImportNewSongService` (already-implemented, previously untested) into one
  workflow. Same service-injects-concrete-service composition pattern
  `UpdateSongsService` already uses for `UpdateSongService` - no new
  "workflow framework" was built for this; see discussion notes below.
- `TransposeCommand` rewritten for the new interface - the old
  `--output`-based interface is gone, not kept alongside.
- Guard rails, checked before anything is written:
  1. Duplicate-key guard - refuses to create a variant whose musical key
     already exists anywhere in the song's group, enharmonic-aware (compares
     `MusicalKey` values, not raw catalog strings or filenames) - catches,
     for example, transposing a `-b` variant back down to a key the base
     file already covers, even if the existing catalog row spells it `C#`
     and the newly-computed one would spell it `Db`.
  2. Overwrite guard - refuses to write over an existing target file.
- Import-failure handling (deliberate, not a bug): if the transpose succeeds
  but the subsequent catalog import throws, the `.cho` file is never rolled
  back - it's valid content and stays in place. The failure is reported with
  the exact `./import-song <path>` command needed to finish the job by hand.
  This was a real decision, not an oversight - the alternative (auto-delete
  a successfully-written file because a separate step failed) was
  considered and rejected as more surprising, not less.
- Also closed, as prep work: `TransposeService`/`ImportNewSongService` no
  longer call `System.out`/`System.err` directly - both now return
  structured results (`TransposeResult`/`ImportResult`) and their respective
  CLI adapters own 100% of the presentation. This was a real hexagonal-
  boundary leak (domain services printing) that had to close before
  composing them, since a composed workflow can't sensibly decide what to
  print if its collaborators already printed on its behalf.

On "should this be a generic workflow/pipeline framework?" - considered
and explicitly rejected for now. Two composed steps doesn't justify
`Workflow`/`Step`/pipeline infrastructure; direct service composition (a
service class injecting other concrete services, exactly like
`UpdateSongsService`) is treated as the established convention, not a gap.
Revisit only once a third or fourth real orchestrator shows up and a
genuine repeated shape emerges.

Testing: `ChordProPathTest` and `SongIdTest` added (neither had direct
coverage before - both are central to the whole SONG ID system).
`ImportNewSongServiceTest` added (5 tests - previously zero coverage).
`TransposeSongServiceTest` added (9 tests): happy path, deriving a sibling
from an existing non-base variant, `--no-import`, import-failure handling,
missing-source-file guard, existing-target-file guard, and two
key-collision guard tests (same spelling + genuine enharmonic spelling).
Also manually smoke-tested end-to-end against real catalog data
(`ABC:B:BillyJoel:MovinOut`, Dm to Em, +2 semitones): chord qualities
preserved correctly (`Em7-5` to `F#m7-5`), catalog row appended correctly,
then reverted cleanly. 204/204 automated tests passing.

**Guardrail decision:** rather than build a separate lint tool or duplicate
the chord-validity grammar in `lint-cho.zsh`, Phase 1's `transpose` command
will print a loud warning (not a hard failure) whenever it encounters a
bracket that looks like a chord attempt but fails the quality grammar — see
§6 above. Catches future regressions of this exact kind at the point they'd
actually matter, with zero duplicated logic.
