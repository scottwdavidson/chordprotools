
# Design: Song Characterization & Venue Fit

> **Status:** Draft for review · **Author:** Kino (from a Scott brainstorm session) · **Date:** 2026-09-06
> **Scope:** New song metadata (energy, vocal intensity, genre, sing-along) +
> a venue-policy concept + a setlist validator/scorer. Does **not** cover
> auto-generating a setlist — that's explicitly deferred (see §8).

---

## 1. Problem statement

Scott builds setlists by hand, gig by gig, and has strong intuitions about
what works where:

- A **wine bar** wants subdued/low music but can include some sung songs.
- A **restaurant** wants essentially no vocals.
- An **outdoor gig** (including an outdoor wine bar) tolerates more "rock"
  energy than either of the above — e.g. *You Wreck Me* (Tom Petty), *Cover
  Me* (Springsteen), *Bad Case of Lovin' You* (Robert Palmer), *Mary Jane's
  Last Dance* (Tom Petty).
- A good setlist has a **shape**: skew mellower early, sprinkle some upbeat
  songs in early, then dominate the final third with upbeat/sing-along
  songs to build crowd energy toward the end.

None of this is captured today. `CatalogEntry` has musical/technical metadata
(`key`, `tempo`, `timeSignature`, `backingType`, …) but nothing describing
**vibe**. There's no way to programmatically check "does this setlist fit
this venue" or "does the energy arc make sense."

---

## 2. Goals & non-goals

### Goals (this doc)
- Add song-level metadata: **energy level**, **vocal intensity**, **genre
  (primary + optional secondary)**, **sing-along flag**.
- Add a **venue policy** concept (max vocal intensity + energy ceiling),
  attached per gig, independent of song data.
- Build `evaluate-setlist` — a read-only validator/scorer that checks an
  already-assigned gig against its venue policy and reports:
  - hard violations (song exceeds vocal/energy limits for the venue)
  - arc shape (does energy trend upward across the set, are sing-alongs
    clustered near the end)
- Build `characterize-songs` — a bulk-backfill helper to populate the new
  fields across the ~300+ song catalog without a fully manual pass.

### Non-goals (this doc)
- **Auto-generating a setlist.** Deferred until `evaluate-setlist`'s scoring
  model has been checked against several real gigs and Scott trusts it.
  Building a generator on top of an unproven scoring model risks confidently
  automating the wrong thing.
- Gating anything on **genre**. Genre is descriptive/variety metadata only
  (see §3.3) — never a hard constraint. Modern "Country" material overlaps
  too much with 70s/80s rock for genre-based gating to make sense.
- Real-time/live setlist reordering during a gig. Out of scope entirely.

---

## 3. New song metadata — three independent axes

The brainstorm conflated "how rocking is it," "does it have vocals," and
"what genre is it" into one blob. They don't move together (a mellow
acoustic song can still have full lead vocals; energy and genre are
uncorrelated for this catalog specifically per Scott's own observation about
new Country material). Keep them orthogonal.

### 3.1 Energy level — `energyLevel: Integer` (1–10)

Continuous scale from mellow ballad to dance/upbeat. Chosen over a coarser
1–5 scale so the averaging/median math in §5 has enough resolution to be
meaningful rather than mushy.

| Range | Label | Calibration example |
|---|---|---|
| 1–2 | Mellow ballad | *Today's the Day* (America), *Sailing* (Christopher Cross) |
| 3–4 | Medium ballad / soft pop | — |
| 5–6 | Pop / mid-tempo | — |
| 7–8 | Rock | *You Wreck Me*, *Cover Me*, *Bad Case of Lovin' You*, *Mary Jane's Last Dance* |
| 9–10 | Dance / upbeat | — |

### 3.2 Vocal intensity — `vocalIntensity: enum {NONE, LIGHT, FULL}`

The real venue constraint from Scott's brainstorm — separate from energy
entirely:

| Value | Meaning |
|---|---|
| `NONE` | Instrumental-friendly; no lead vocal required |
| `LIGHT` | Some/occasional or quiet vocals |
| `FULL` | Full lead vocal throughout |

Ordinal, so venue policy can do a simple `song.vocalIntensity <=
venue.maxVocalIntensity` comparison.

### 3.3 Genre — `genrePrimary: String`, `genreSecondary: String` (optional)

Descriptive only. Never used to block or require song placement. Reserved
for future **variety** checks (e.g. "don't stack three genre-primary=Country
songs back to back") — not built in this phase, just keeping the field
around so it doesn't need to be bolted on awkwardly later.

### 3.4 Sing-along flag — `singAlong: Boolean`

Crowd-engagement marker. Used by the arc scorer (§5) to check placement
near the end of the set, independent of energy level (a sing-along could in
principle be a medium-energy song the crowd just loves).

---

## 4. Venue policy — decoupled from the song, attached to the gig

Venue policy is **data, not code** — no hardcoded `RESTAURANT`/`WINE_BAR`
enum baked into the codebase (brittle; a new venue archetype shows up the
first week someone books something unusual). Instead, a policy is just two
knobs, one row per gig:

**New file: `venue-profiles.csv`**

```
GIG,MAX_VOCAL_INTENSITY,ENERGY_CEILING,ENERGY_FLOOR
2026-09-06-SomeRestaurant,NONE,4,1
2026-09-06-WineBar,LIGHT,6,1
2026-09-06-Outdoor,FULL,10,2
```

- `ENERGY_FLOOR` is optional (defaults to 1 / no floor) — included for
  completeness but not expected to matter much in practice.
- Kept **separate from `gigs.csv`** rather than added as columns there,
  because `gigs.csv` is long-format (one row per song per gig, ~150 rows per
  gig) — repeating a gig-constant value on every song row would be pure
  duplication for no benefit (DRY).
- A gig with no matching row in `venue-profiles.csv` has no policy —
  `evaluate-setlist` should treat that as "no venue constraints configured,
  skip hard-violation checks, arc-shape check still runs."

---

## 5. `evaluate-setlist` — the scoring algorithm

Given a gig slug, join `gigs.csv` → `song-catalog.csv` (via `SetlistEntry`,
already exists) → `venue-profiles.csv`.

### 5.1 Hard violations (correctness, not style)

For every assigned song:
- `song.vocalIntensity > venue.maxVocalIntensity` → flag by set position + title
- `song.energyLevel > venue.energyCeiling` (or `< energyFloor`) → flag

These are reported as a straight list — not a "score," a bug report.

### 5.2 Arc shape (soft scoring)

1. Split the ordered setlist into thirds (or reuse existing set-block
   letters A/B/C/Z if that's a cleaner boundary — TBD, see open questions).
2. Compute average + median `energyLevel` per third.
3. Validate the expected shape: first third skews lower with occasional
   upbeat sprinkles, final third average is meaningfully higher than first
   third — **unless** the venue's `energyCeiling` makes a real arc
   impossible (e.g. a restaurant capped at 4 across the board), in which
   case skip the arc-shape expectation entirely and just report "flat by
   venue design."
4. **Sing-along clustering check**: flag if `singAlong=true` songs aren't
   concentrated in the final third, and specifically check whether at least
   one of the last 2–3 songs is a sing-along.

### 5.3 Report format (sketch, following the house style)

```
evaluate-setlist --gig 2026-09-05-Balboa

  venue policy: MAX_VOCAL_INTENSITY=FULL, ENERGY_CEILING=10

  [VIOLATION] C05 RobertPalmer:BadCaseOfLovinYou — energyLevel 8 > ceiling 6
              (only relevant if venue policy caps lower than this gig's actual policy)

  arc shape:
    first third   avg energy 4.2  median 4
    middle third  avg energy 5.8  median 6
    final third   avg energy 7.9  median 8
    [OK] upward trend as expected

  sing-along placement:
    [OK] 2 sing-along song(s) in final third
    [OK] last 3 songs include a sing-along (TomPetty:MaryJanesLastDance)

  -> 0 hard violation(s), arc shape OK
```

Exit code = violation count (0 = clean), consistent with the
`consistent-song-data` precedent (CI-friendly, scriptable).

---

## 6. `characterize-songs` — bulk backfill helper

Adding 4 new fields to a 300+ song catalog can't reasonably be done by hand,
one row at a time — same lesson learned from the `bracket-chords` rollout
(2,397 bare chords across 359 files needed automation + a flagged manual
tail, not a fully manual pass).

Proposed shape, mirroring `bracket-chords --fix`:

1. First pass: heuristic/LLM-assisted pre-fill of `energyLevel`,
   `vocalIntensity`, `genrePrimary` based on title/artist pattern matching
   against known songs.
2. Report what's still unrated or low-confidence, for manual review —
   editable in Excel/Sheets exactly like the rest of `song-catalog.csv`
   already is (`tidy-song-catalog` re-import flow, no new UI needed).
3. `--dry-run` default; never silently overwrite an already-set value.

**Not building this until schema (§3) is approved** — don't want to write
backfill tooling against a schema that might still change shape.

---

## 7. Where this fits (hexagonal architecture)

```
adapter/in/file/EvaluateSetlistCommand.java          ← picocli @Command
adapter/in/file/CharacterizeSongsCommand.java        ← picocli @Command
        │
        ▼
application/port/in/EvaluateSetlistUseCase.java
application/port/in/CharacterizeSongsUseCase.java
        │
        ▼
application/domain/service/EvaluateSetlistService.java
        │   uses CatalogPort, GigsPort (existing), VenueProfilePort (new)
        │   uses SetlistArcScorer (new)
application/domain/service/CharacterizeSongsService.java
        │
        ▼
application/domain/model/
    CatalogEntry.java          (extend: energyLevel, vocalIntensity,
                                 genrePrimary, genreSecondary, singAlong)
    VenueProfile.java          (new)
    VocalIntensity.java        (new enum)
    SetlistEvaluationReport.java (new)
```

New collaborators:

| Class | Responsibility |
|---|---|
| `VenueProfilePort` / adapter | Read `venue-profiles.csv` |
| `VenueProfile` | Model: gig slug → max vocal intensity, energy ceiling/floor |
| `SetlistArcScorer` | Splits an ordered `SetlistEntry` list into thirds, computes avg/median energy, checks trend + sing-along placement |
| `SetlistEvaluationReport` | Findings: violations + arc summary |

---

## 8. Proposed build order (phased — do not skip ahead)

1. **Phase 0 — schema only.** Add the 4 new fields to `CatalogEntry` +
   `song-catalog.csv` (all nullable/optional, nothing breaks). Add
   `VocalIntensity` enum. **Get this reviewed before anything else** — it's
   the one decision that's expensive to redo across 300+ rows.
2. **Phase 1 — `venue-profiles.csv` + `VenueProfile` model.** Small, low
   risk, a couple of real example rows seeded from the actual gig list.
3. **Phase 2 — `evaluate-setlist` (read-only).** Ships value immediately
   without touching how setlists are built at all. This is the safe,
   high-value deliverable — validate here before building anything that
   writes/generates.
4. **Phase 3 — `characterize-songs` (bulk backfill).** Only after schema is
   stable; populates the catalog so Phase 2 has real data to chew on.
5. **Phase 4 — auto-suggest / generate a setlist.** Explicitly deferred
   (YAGNI) until Phase 2's scoring has been checked against several real
   gigs and actually matches Scott's gut instincts. Do not build a
   generator on an unproven scorer.

---

## 9. Open questions for Scott

1. **Energy scale confirmed as 1–10?** (vs. a coarser 1–5). Locking this in
   before any backfill work starts.
2. **`venue-profiles.csv` as a separate file — confirmed?** (vs. extra
   columns repeated per song row in `gigs.csv`).
3. **Arc thirds**: split by raw song count into thirds, or align to the
   existing set-block letters (A/B/C/Z) that are already meaningful set
   breaks? Might matter for gigs with uneven block sizes.
4. **`characterize-songs` first-pass heuristic**: is an LLM-assisted
   pre-fill (per artist/title pattern) trustworthy enough to run against
   the whole catalog with a manual-review tail, or would Scott rather do a
   fully manual first pass on a smaller "greatest hits" subset first and
   expand later?
5. **Missing venue profile behavior**: confirmed that a gig with no
   `venue-profiles.csv` row just skips hard-violation checks (no default
   policy assumed)?

---

## 10. Relationship to existing docs / commands

- Builds on top of `CatalogEntry`/`SetlistEntry`/`gigs.csv` — no changes to
  those existing join mechanics, only new fields/files layered alongside.
- Independent of the `stabilize-song-body-delimiter.md` /
  `consistent-song-data.md` work — that's about chart *body* content
  correctness; this is about setlist *composition* correctness. No shared
  code paths, safe to build in either order.
