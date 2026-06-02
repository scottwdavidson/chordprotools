# Design: `consistent-song-data`

> **Status:** Draft for review · **Author:** Kino 🐶 · **Date:** 2026-06-02
> **Scope:** Body/content consistency across key-variants — the chords and
> lyrics, not just the catalog metadata.
> **Sibling:** `consistent-metadata.md` (catalog-level check). Read that first.

---

## 1. Problem statement

Two key-variants of a song should have **the same musical content** — same
structure, same lyrics, same chord *shapes* — differing **only by a consistent
transposition**. Example from the real library:

`HollywoodNights.cho` (key E) vs `HollywoodNights-b.cho` (key B), down a
perfect fourth (or up a fifth):

```
E version :  [E5] She stood there bright as the sun on that [A/E]California coast.
B version :  [B5] She stood there bright as the sun on that [E/B]California coast.
```

Every `E5→B5`, `A/E→E/B`, `D/E→A/B` … is the **same** −5 (or +7) semitone shift.
If the variants are truly the same arrangement, transposing one by the key
interval should reproduce the other **exactly**, modulo:

- **Enharmonic spelling** (`A#` vs `B♭`) — must be treated as equal.
- **Version-specific annotations** — e.g. a guitar-specific comment that should
  live in only one variant. (Deferred — see §7.)

We want to **detect** the real differences between two variants (a structural
or lyrical drift that *isn't* explained by transposition) and optionally
**fix** a variant by regenerating it from a chosen source of truth.

---

## 2. Goals & non-goals

### Goals
- `consistent-song-data <songId>` compares the variants of a song group.
- `--dry-run` (default): show a **transposition-aware diff** — i.e. diff the
  variants *after* normalising them to a common key, so pure key differences
  vanish and only genuine drift shows.
- `--fix --source <songId>`: regenerate the other variant(s) by transposing the
  source body to each target's key, preserving each target's own `{key:}` and
  metadata.
- Reuse the existing `ChordProTransposer` (it already parses `[chord]` tokens
  and shifts by half-steps).

### Non-goals (v1)
- Version-specific annotation system (guitar-only comments, etc.) — designed
  for later (§7), not built now.
- Auto-detecting the transposition interval if the keys are wrong/missing —
  v1 derives the interval from the `{key:}` directives; if those are wrong,
  `consistent-metadata` should catch it first.
- Reflowing / re-formatting whitespace beyond what's needed to compare.

---

## 3. Core idea: normalise-then-diff

The whole feature hinges on one transformation:

```
canonicalise(variant) = transpose(variant.body, -interval(variant.key → reference))
```

Pick a **reference key** (say, the source variant's key). Transpose every
variant's body *to that reference key*. Now two variants that differ only by
transposition become **byte-identical** (after enharmonic normalisation), and a
plain line diff reveals only the real differences.

```
        ┌─ HollywoodNights.cho   (E) ─┐ transpose 0  ─┐
group → │                              │               ├─→ compare in key E
        └─ HollywoodNights-b.cho (B) ─┘ transpose +5 ─┘
```

This is far more robust than trying to diff the raw files (which would scream
about every single chord).

---

## 4. Where this fits (hexagonal architecture)

```
adapter/in/file/ConsistentSongDataCommand.java      ← picocli @Command
        │  flags: --fix, --source <songId>, --dry-run (default)
        ▼
application/port/in/ConsistentSongDataUseCase.java   ← interface
        ▼
application/domain/service/ConsistentSongDataService.java
        │  uses CatalogPort (find group), ChordProPort (read/write bodies)
        │  uses ChordProTransposer (the engine)
        │  uses SongBodyCanonicaliser (new) + SongBodyDiffer (new)
        ▼
application/domain/model/SongDataConsistencyReport.java
```

New collaborators:

| Class | Responsibility |
|---|---|
| `SongBodyCanonicaliser` | Given a `ParsedSong` and a target key, return the body transposed to that key (delegates per-line to `ChordProTransposer`). |
| `SongBodyDiffer` | Line-level diff of two canonicalised bodies; classifies each diff (see §5). |
| `KeyInterval` | Computes半-step distance between two keys (reuse chromatic math from `ChordProTransposer`). |

---

## 5. What counts as a "real" difference?

After canonicalising to a common key, we diff line by line. Each differing line
is classified:

| Class | Meaning | `--fix` action |
|---|---|---|
| **CHORD DRIFT** | Same lyric, different chord (even after transposition) — a genuine harmonic disagreement. | Overwrite target line from source. |
| **LYRIC DRIFT** | Different words / structure. | Overwrite target line from source. |
| **STRUCTURE DRIFT** | A whole section/line present in one, absent in the other (`{start_of_verse}` count differs, etc.). | Overwrite whole body from source. |
| **VERSION NOTE** | A `{comment:}` line tagged as version-specific (§7). | **Preserve** — never overwritten. |
| **ENHARMONIC** | Lines differ only by spelling (`A#`/`B♭`). | None — treated as equal, not a diff. |

The enharmonic class is the trickiest: rather than string-comparing the
canonicalised lines, we should compare them **chord-by-chord by chromatic
position** so `[A#]` and `[Bb]` are equal. That means `SongBodyDiffer` needs to
understand chords, not just text — it can reuse `ChordProTransposer`'s chord
regex + `getNotePosition()`.

---

## 6. Report format (`--dry-run`)

```
consistent-song-data ABC:B:BobSeger:HollywoodNights (dry-run)
  reference key: E  (from source HollywoodNights.cho)

  comparing HollywoodNights-b.cho (B → transposed +5 to E)

  ✓ structure matches (8 sections, 136 lines)
  ✓ no chord drift
  ✓ no lyric drift

  → variants are consistent (transposition-only difference)

----------------------------------------------------------------
consistent-song-data ABC:C:AvrilLavigne:Complicated (dry-run)
  reference key: F#m (from source Complicated.cho)

  comparing Complicated-em.cho (Em → transposed +2 to F#m)

  [LYRIC DRIFT]  line 42
      source:  Tell me [A]why'd you have to go and make things so complicated
      target:  Tell me [A]why you had to go and make things so complicated
  [CHORD DRIFT]  line 55
      source:  [F#m]  chillin' [D]out
      target:  [F#m]  chillin' [Bm]out

  → 2 difference(s) not explained by transposition
```

Exit code = total real differences (0 = clean), CI-friendly.

---

## 7. Version-specific annotations (deferred design sketch)

The requirement: sometimes a variant legitimately needs its own note (e.g. "use
drop-D tuning here" for the guitar version) that should **not** be flagged as
drift or overwritten by `--fix`.

**Proposed convention (future):** a tagged comment directive, e.g.

```
{comment: @only(HollywoodNights-b) capo 2 to match horns}
```

or a dedicated directive:

```
{meta: version-note: capo 2 to match horns}
```

`SongBodyDiffer` would recognise these and bucket them as **VERSION NOTE** —
excluded from drift detection, preserved across `--fix`. **Not built in v1**;
we just make sure the diff classifier has a clean extension point for it.

---

## 8. `--fix` mode

```
consistent-song-data <groupId> --fix --source <sourceSongId>
```

Algorithm per target variant (every variant except the source):

1. Read source body + target `{key:}`.
2. Compute interval `source.key → target.key`.
3. Transpose source body by that interval (`ChordProTransposer`).
4. Re-attach the **target's own header** (its key + its metadata) to the newly
   transposed body.
5. Preserve any **VERSION NOTE** lines from the target (once §7 lands).
6. Write via `ChordProPort`.

> ⚠️ **This overwrites the target's body.** `--fix` must:
> - require an explicit `--source`,
> - default to dry-run,
> - print the diff and a confirmation summary,
> - rely on git as the safety net (commit before fixing).

**Flat/sharp preference:** when transposing, which spelling do we emit? Derive
from the target key signature (sharps for sharp keys, flats for flat keys).
`ChordProTransposer.transpose(line, halfSteps, useFlats)` already takes a
`useFlats` flag — we just choose it from the target key.

---

## 9. Hard problems / risks (be honest)

1. **Slash chords & bass notes** — `[A/E]` has *two* notes; both must transpose.
   `ChordProTransposer`'s regex matches only the **leading** chord token per
   `[...]`, so `[A/E]` currently transposes `A` but **not** the `/E` bass.
   👉 **This is a real gap.** Verify and likely extend the transposer to handle
   slash chords before `--fix` can be trusted. (Detection/dry-run is safe; it's
   `--fix` that would mangle bass notes.)
2. **Non-chord bracket content** — e.g. `[2x]`, `[Instrumental]`. The regex
   keys on `[A-G]`, so `[2x]` is safe, but `[Bridge]` starts with `B`… 👉 needs
   a guard (only treat as a chord if it parses as a valid chord).
3. **Whitespace / alignment** — chord positions in monospace charts may shift by
   a character when a chord name changes width (`[A]`→`[A#]`). Do we care about
   exact column alignment? **Recommendation:** compare *semantically* (chords +
   lyrics), not column-for-column, to avoid false positives.
4. **Capo interaction** — a capo'd variant might intentionally use different
   *shapes* for the same sounding pitch. 👉 If `{capo:}` differs between
   variants, transposition-equality breaks down. v1 should **detect capo
   mismatch and refuse `--fix`**, deferring to the human.
5. **Performance** — full-catalog scan transposes every body; fine for a few
   hundred songs, but the default should probably be **single-song**
   (`consistent-song-data <songId>`) with an opt-in `--all`.

---

## 10. Proposed new/changed files

| File | Type | Notes |
|---|---|---|
| `ConsistentSongDataUseCase.java` | port/in | `int check(opts)` |
| `ConsistentSongDataService.java` | service | orchestration |
| `ConsistentSongDataCommand.java` | adapter | flags |
| `SongBodyCanonicaliser.java` | service | transpose body to reference key |
| `SongBodyDiffer.java` | service | classify diffs (chord/lyric/structure/enharmonic) |
| `KeyInterval.java` | model | semitone distance between keys |
| `SongDataConsistencyReport.java` | model | findings |
| `ChordProTransposer.java` (extend) | service | **slash-chord support**, chord-validity guard |
| `./consistent-song-data` | shell | shim → `./cpt` |
| Tests | test | transposition round-trip, enharmonic, slash chords, drift classes, fix |

---

## 11. Suggested build order (phased)

1. **Phase 0 — harden the transposer.** Slash-chord support + chord-validity
   guard + tests. Everything else depends on this being correct.
2. **Phase 1 — detection (dry-run only).** Canonicaliser + differ + report.
   Read-only, safe, immediately useful. Ship this first.
3. **Phase 2 — `--fix`.** Body regeneration with capo-mismatch refusal and git
   safety messaging.
4. **Phase 3 — version-specific annotations (§7).** Only once the convention is
   agreed.

---

## 12. Open questions for Scott

1. **Default scope** — single-song (`<songId>` required) with opt-in `--all`,
   or scan everything by default? I lean single-song for the heavy one.
2. **Slash chords** — confirm the transposer gap matters for your charts (it
   clearly does for Hollywood Nights — lots of `[A/E]`). This gates `--fix`.
3. **Capo mismatch** — agree we refuse `--fix` and just report when `{capo:}`
   differs between variants?
4. **Alignment** — semantic compare (ignore column drift) acceptable, or do you
   want chord charts to stay column-aligned?
5. **Version notes** — is the `{comment: @only(...)}` convention appealing, or
   do you have a preferred syntax? (Deferred either way.)
6. **Reference key** — always the `--source` variant's key, or always
   canonicalise to the **base** variant's key regardless of source?

---

## 13. Relationship to `consistent-metadata`

Run order matters: **metadata first, then song data.**
`consistent-song-data` *trusts* the `{key:}` directives to compute intervals, so
the keys must be correct — which is exactly what `consistent-metadata`'s Check B
(filename/key match) guarantees. Consider a future `--with-metadata` umbrella,
but keep them as two focused commands for now (Single Responsibility).
