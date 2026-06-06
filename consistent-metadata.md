# Design: `consistent-metadata`

> **Status:** Draft for review · **Author:** Kino 🐶 · **Date:** 2026-06-02
> **Scope:** Catalog-level consistency check across key-variants of the same song.

---

## 1. Problem statement

A song can exist in multiple key-variant files, e.g.:

| Song ID | File | Key |
|---|---|---|
| `ABC:B:BobSeger:HollywoodNights`   | `HollywoodNights.cho`   | E |
| `ABC:B:BobSeger:HollywoodNights-b` | `HollywoodNights-b.cho` | B |

These are the **same song** in different keys. Their catalog metadata
(duration, tempo, count-in, backing, label, …) **should be identical** — the
*only* legitimate difference is the musical **key**.

Today nothing enforces this. A bandmate can edit one variant's duration, add a
count-in to only one file, or transpose a file without updating its `{key:}`
directive, and the catalog silently drifts apart.

We want a command that **detects** (and optionally **fixes**) two classes of
inconsistency:

1. **Cross-variant metadata drift** — any non-key field differs between
   variants of the same song.
2. **Filename/key mismatch** — when a variant's filename encodes a key
   (the `-b`, `-c#m`, `-am` suffix), the `{key:}` *inside* that variant must
   match the key in its filename.

---

## 2. Goals & non-goals

### Goals
- A `consistent-metadata` command that scans **every** song group with 2+
  variants and reports inconsistencies.
- `--dry-run` (default) prints a human-readable report; exits non-zero if any
  issues found (CI-friendly, same convention as `verify-catalog`).
- A `--fix` mode that repairs drift by propagating from a chosen source of
  truth (see §6).
- Reuse existing domain machinery: `SongId.toGroupKey()`, the grouping pattern
  from `FindSongIdService`, and the field-diff pattern from
  `VerifyCatalogService`.

### Non-goals
- Checking the **song body / chords** — that's the sibling feature
  `consistent-song-data`.
- Inventing new metadata fields.
- Touching `gigs.csv` / RC-slot assignments (per-gig, not per-song).

---

## 3. Where this fits (hexagonal architecture)

Mirrors every other command in the codebase:

```
adapter/in/file/ConsistentMetadataCommand.java     ← picocli @Command
        │  (parses --dry-run / --fix / --source flags)
        ▼
application/port/in/ConsistentMetadataUseCase.java  ← interface
        ▼
application/domain/service/ConsistentMetadataService.java
        │  reads CatalogPort, groups by SongId.toGroupKey()
        ▼
application/domain/model/MetadataConsistencyReport.java  ← immutable result
```

Plus a top-level shell shim `./consistent-metadata` delegating to `./cpt`.

**This is a catalog-only feature** — it reads `song-catalog.csv` via
`CatalogPort` and (in `--fix` mode) writes it back, then the user runs
`update-song` to push to the `.cho` files. No new ports required.

---

## 4. The two checks in detail

### Check A — cross-variant metadata drift

For each song **group** (all entries sharing `SongId.toGroupKey()`):

1. Collect all `CatalogEntry` rows in the group.
2. Compare every field **except** `KEY` (the lever that
   legitimately differs per-variant) across all variants.
3. Any field whose normalised value isn't identical across all variants is a
   **DRIFT** finding.

> **Why `PERFORMANCE KEY` is compared (not excluded).** The performance key is
> the *sounding* key everyone actually plays in — it must be identical across
> variants. Example: the standard variant is written in key **C**;
> the guitarist's variant is written in key **B♭**. Both sound
> in **C**, so `PERFORMANCE KEY = C` for both. The written `{key:}`
> is the only thing that may differ; everything else — including
> performance key — must match.

Reuses the exact field list and `normalise()` logic already in
`VerifyCatalogService.diff()` — we should **extract that into a shared
`CatalogEntryComparator`** so we don't copy-paste it (DRY). That's a small
refactor of `verify-catalog` that both features benefit from.

> **Fields compared:** TITLE, ARTIST, DURATION, TEMPO, TIME SIG, COUNTIN,
> NORD, ROLAND, VE, BACKING, SONG LABEL, **PERFORMANCE KEY**.
> **Fields excluded:** KEY (per-variant lever), RC SLOT (per-gig).

### Check B — filename key vs file key

`SongId` already parses the trailing key token into `keyAlternative`
(`KEY_ALT_PATTERN = -([a-gA-G][#b]?m?)`). So for any **variant** (non-base)
entry:

1. `songId.getKeyAlternative()` → e.g. `"b"`, `"c#m"`, `"am"`.
2. Normalise it to a canonical key (`B`, `C#m`, `Am`).
3. Compare against the entry's `{key:}` value.
4. Mismatch → **FILENAME/KEY** finding.

**Subtlety — note normalisation.** The filename token is lowercase and the
catalog key is mixed-case, and we must handle:
- Case: `-b` ⇒ key `B`.
- Minor: `-am` ⇒ `Am`, `-c#m` ⇒ `C#m`.
- Enharmonics: should `-bb` (B♭) match a catalog key of `A#`? **Yes** — we
  should treat enharmonic equivalents as equal. We already have an enharmonic
  map + chromatic position logic in `ChordProTransposer.getNotePosition()`.
  **Recommendation:** extract a tiny `MusicalKey` value object that both this
  feature and the transposer use, so key-equality is defined in exactly one
  place. (See Open Questions.)

> **Base-version note:** the base file (`HollywoodNights.cho`, no suffix) has no
> filename key to check — Check B only applies to suffixed variants. Its key is
> whatever the catalog says (the "standard" key).

---

## 5. Report format (`--dry-run`)

```
consistent-metadata (dry-run)

[DRIFT] ABC:B:BobSeger:HollywoodNights  (2 variants)
  COUNTIN   HollywoodNights='8'  HollywoodNights-b='' 
  TEMPO     HollywoodNights='150'  HollywoodNights-b='148'

[FILENAME/KEY] ABC:C:AvrilLavigne:Complicated-em
  filename key='Em'  catalog key='E'

consistent-metadata: 47 groups checked, 45 consistent, 2 issue(s)
```

Exit code = number of issue groups (0 = clean). Matches `verify-catalog`.

---

## 6. `--fix` mode — source of truth

Drift can't be auto-fixed without deciding **which variant is right**. Options:

| Strategy | Behaviour |
|---|---|
| `--source base` (default) | The base (standard-key) variant's metadata wins; copy its non-key fields to all variants. Fails if the group is orphaned (no base). |
| `--source <songId>` | Named variant is the source of truth. |
| (no `--fix`) | Report only. |

For **Check B** (filename/key), `--fix` is more delicate: the filename token and
the `{key:}` disagree, but which is correct? **Recommendation:** *do not*
auto-fix filename/key mismatches in v1 — only report them. Renaming a file or
rewriting a key has real musical consequences and ties into transposition
(`consistent-song-data` territory). Flag it; let the human decide.

> `--fix` writes `song-catalog.csv` only. The user then runs `update-song
> <groupId>` (which already fans out to all variants — nice synergy with the
> recent refactor!) to push the corrected metadata into the `.cho` files.

---

## 7. Proposed new/changed files

| File | Type | Notes |
|---|---|---|
| `ConsistentMetadataUseCase.java` | port/in | `int check(ConsistentMetadataOptions opts)` |
| `ConsistentMetadataService.java` | service | grouping + both checks |
| `ConsistentMetadataCommand.java` | adapter | flags: `--fix`, `--source` (whole-catalog scan; no `--song`) |
| `MetadataConsistencyReport.java` | model | immutable findings list |
| `CatalogEntryComparator.java` | service | **extracted** from VerifyCatalogService (DRY) |
| `MusicalKey.java` | model | canonical key + enharmonic equality (built now) |
| `./consistent-metadata` | shell | shim → `./cpt consistent-metadata "$@"` |
| `VerifyCatalogServiceTest` (touch) | test | after the comparator extraction |
| `ConsistentMetadataServiceTest` | test | drift + filename/key + fix cases |

---

## 8. Test scenarios

1. Group with identical metadata except key → **clean**.
2. Group where one variant has a different `tempo` → **DRIFT**.
3. Group where one variant has a different `performance key` → **DRIFT**
   (performance key is an invariant).
4. Group where variants differ only by `key` →
   **clean**.
5. Variant `-b` whose `{key:}` is `E` → **FILENAME/KEY** (report only).
6. Variant `-bb` whose key is `A#` → **clean** (enharmonic).
7. Orphan group (variants only, no base) with `--fix --source base` → **error**.
8. `--fix --source base` propagates count-in to a variant missing it.
9. Single-variant song → skipped (nothing to compare).

---

## 9. Resolved decisions (Scott, 2026-06-02)

1. **Performance key is an invariant** — it must match across variants (it's the
   sounding key). `KEY` is the per-variant lever and is the only
   field excluded from drift. *(Doc updated in §4.)*
2. **Enharmonic equality** — `-bb` (B♭) ≡ catalog key `A#`. Treated as the same
   key. (Unlikely in practice, but handled correctly.)
3. **`MusicalKey` value object** — yes, build the OO abstraction now; it owns
   canonical key parsing + enharmonic equality in one place and serves
   `consistent-song-data` later too.
4. **`--fix` for filename/key** — **report only**, never auto-fix. Since we're in
   GitHub, a human renames the file / corrects the key.
5. **Default mode** — `--dry-run` is the default; `--fix` must be explicitly
   requested.
6. **Scope** — always scan the **whole catalog**. No `--song` flag. This is a
   periodic "tidy" command; the user already knows when one is off and doesn't
   need targeted discovery.

---

## 10. Why this is low-risk

- Read-only by default; `--fix` only touches `song-catalog.csv` (which is
  already version-controlled and roll-back-friendly).
- Reuses three existing, tested patterns (grouping, field-diff, key math).
- The only refactor (`CatalogEntryComparator` extraction) makes
  `verify-catalog` cleaner too — a net DRY win.
