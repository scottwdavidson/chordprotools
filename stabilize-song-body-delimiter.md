# Design: `stabilize-song-body-delimiter`

> **Status:** Draft for review · **Author:** Kino 🐶 · **Date:** 2026-06-04
> **Scope:** Insert an explicit, non-displaying delimiter line into every `.cho`
> file that unambiguously separates the **header region** (metadata directives +
> the OnSong-visible metadata comment banners) from the **song body** (chords +
> lyrics + structure).
> **Relationship:** A prerequisite/foundation step *before* `consistent-song-data.md`.
> Not the same effort — that one diffs song bodies; this one just draws a clean
> line in the sand so "where does the body start?" stops being a guess.
> **⚠️ NOT a today task — Scott has a gig tomorrow. This is the plan only.**

---

## 1. Why we need this (the motivating mishap)

The recent duplicate-metadata incident (`UnderTheMilkyWay.cho` et al.) happened
because **there is no explicit boundary** between a song's metadata and its body.
Today the boundary is *inferred* by `SongParser`:

```
adapter/.../SongParser.java  (current behaviour)
  ── read lines top-to-bottom
  ── for each line, ask SongLineParser "is this a header directive?"
  ── the FIRST line that doesn't parse as a header  →  body starts here
```

This "parse until something unrecognized" heuristic is fragile:

- A stray/duplicated `{meta: …}` block, a deprecated `{meta: version: …}`, or a
  reordered comment banner can shift where the parser *thinks* the body begins.
- Tools that rewrite the header (`update-song`, `assign-backing-track-slots`)
  rely on this inferred boundary, so corruption above the line silently
  propagates.
- There is no single, greppable marker a human (or a one-line script) can use to
  say "everything above here is mine to manage; everything below is sacred."

**Fix:** write a literal delimiter line into each file. The boundary becomes a
fact in the file, not a runtime inference.

---

## 2. The delimiter: a ChordPro `#` remark line (non-displaying)

ChordPro (and therefore OnSong) treats a line whose first non-whitespace
character is `#` as a **remark** — a file-level comment that is **never
rendered**. This is distinct from `{comment: …}`, which OnSong *does* display.

> 📌 **Contrast with `{comment:}`**
> | Form | OnSong renders it? | Purpose |
> |---|---|---|
> | `{comment: Intro}` | **Yes** (shown on screen) | musician-facing cue |
> | `{comment: ** meta: nord: P45}` | **Yes** (the metadata banner) | human-readable echo of metadata |
> | `# anything` | **No** (silently ignored) | file-level remark — perfect for a machine delimiter |

This `#` remark is **not yet represented** in our directive enums
(`HeaderDirective`, `SongDirective`) — both are `{…}`-directive oriented. Part of
this work is teaching the parser about it (see §5).

> ❓ **Confirm with Scott:** when you said "see the directive enumerations,"
> did you mean the `#` remark line (my read), or the existing `EPHEMERAL_COMMENT`
> (`**`) marker the tool already uses inside `{comment: ** …}` banners? They
> behave very differently — `#` is invisible in OnSong; `{comment: **}` is
> visible. My plan assumes `#` because you specifically said *"not displayed in
> OnSong."* If you meant something else, this section changes.

### 2.1 The exact sentinel

Human-readable **and** machine-stable. Proposed:

```
#--PCH:SONG-BODY-------------------------------------------------------------
```

- `#` → invisible in OnSong, ignored by every ChordPro renderer.
- `--PCH:SONG-BODY` → a stable, unique token (`PCH` = Pour Choices) that a
  parser/grep can match with zero false positives. No existing `.cho` line
  starts with `#` today (verified — see §9), so the token is collision-free.
- trailing dashes → purely cosmetic, make it visually obvious in a text editor.

Matching rule (lenient on the cosmetic tail, strict on the token):

```
^\s*#--PCH:SONG-BODY
```

---

## 3. The canonical file shape (after stabilization)

```
{title: Under The Milky Way}          ┐
{artist: The Church}                  │
{key: Am}                             │  HEADER REGION
{duration: 5:00}                      │  ── directives (one per key, no dups)
{tempo: 66}                           │
{meta: nord: P45}                     │
{meta: roland: C012}                  │
{meta: countin: 8}                    │
{meta: backing: RC}                   │
{meta: rc-slot: 21}                   │
{meta: label: Under Milky}            │
                                      │
{comment:****************************}│  ← OnSong-visible metadata banner
{comment:**  meta: nord: P45   }      │     (stays ABOVE the delimiter)
{comment:**  meta: roland: C012   }   │
{comment:**  meta: countin: 8   }     │
{comment:**  meta: backing: RC   }    │
{comment:**  meta: rc-slot: 21   }    │
{comment:**  meta: label: Under Milky}│
{comment:****************************}┘
                                      
#--PCH:SONG-BODY----------------------  ← THE DELIMITER (invisible in OnSong)
                                      
{comment: Roland  C022 }              ┐
{comment: Intro}                      │  SONG BODY
| Am . A7sus4 . | Fmaj7/A . G . |     │  ── everything OnSong actually plays
[Am] Sometimes, when this …           │
…                                     ┘
```

**Rule of thumb:** everything the tools *write/manage* (directives + the
metadata echo banner) lives **above** the delimiter. Everything a *musician*
edits (chords, lyrics, section cues like `{comment: Intro}`) lives **below**.

---

## 4. Scope of "header region" — what goes above the line

To avoid ambiguity, the migration tool must classify every pre-body line. The
header region is **exactly**:

1. **Known header directives** (`HeaderDirective` enum): `title`, `artist`,
   `key`, `duration`, `tempo`, `time`, `capo`, and all `{meta: …}` forms
   (`nord`, `roland`, `countin`, `backing`, `rc-slot`, `label`, `ve`,
   `performance`).
2. **The metadata echo banner** — the block of `{comment:****…}` /
   `{comment:** meta: … }` lines (parsed today as `EPHEMERAL_COMMENT`).
3. **Blank lines** interspersed among the above.

**Anything else** encountered before a clearly-musical line is an **anomaly**
(see §7) and must be surfaced, not silently swept above or below the line.

> The first "clearly-musical" line is the first of: a chord/lyric line, a chord
> grid (`| … |`), a section directive (`{start_of_*}`), or a *displaying*
> `{comment: …}` that is **not** a `**` banner (e.g. `{comment: Intro}`).

---

## 5. Parser changes (make the delimiter authoritative)

### 5.1 New directive enum entry

Add a remark concept so the parser recognises `#` lines explicitly rather than
falling through to "unrecognized → body":

```java
// SongDirective.java  (proposed)
REMARK(List.of("#"), List.of(), true);   // non-displaying file remark
```

…and a dedicated constant for the sentinel token so it lives in exactly one
place (DRY):

```java
public static final String SONG_BODY_DELIMITER_TOKEN = "#--PCH:SONG-BODY";
```

### 5.2 `SongParser.parse(...)` — delimiter-first, heuristic-fallback

```
IF the file contains a SONG-BODY delimiter line:
    header  = all lines BEFORE the delimiter
    body    = all lines AFTER  the delimiter   (delimiter line preserved as body's first line, or dropped — see Q in §11)
    → deterministic, no guessing
ELSE  (un-migrated file):
    fall back to the current "parse until non-header" heuristic
    (keeps every not-yet-migrated file working — zero breakage)
```

This makes migration **incremental and safe**: a file works whether or not it
has been stabilized yet. We never need a big-bang flag day.

### 5.3 Writers keep the delimiter

`ChordProFileWriter` / the header-rewriting services (`update-song`,
`assign-backing-track-slots`) must:

- treat the delimiter as the hard top of the body (never write header content
  below it, never write body content above it),
- preserve the delimiter line verbatim on round-trip,
- regenerate the metadata echo banner **above** the delimiter only.

---

## 6. The migration tool (`stabilize-cho`)

A new, **idempotent**, **dry-run-first** utility. Two viable homes — pick per
appetite (see §11):

- **(A) Shell script** in the repo root (like `lint-cho.zsh`) — fast to ship,
  no Java build needed, great for a one-time sweep. Matches the existing
  `lint-cho.zsh` pattern (DRY with that style).
- **(B) Picocli command** (`StabilizeChoCommand` → `…UseCase` → `…Service`)
  following the hexagonal architecture — better if it needs the parser's full
  classification logic and unit tests. Reuses `SongParser`/`SongLineParser`.

**Recommendation:** start with **(A)** for the bulk insert (it's mechanical),
but lean on **(B)** if anomaly classification (§7) needs real parsing. Honestly,
the anomaly detection probably *does* want the Java parser, so (B) is likely the
right long-term home with (A) as a quick stopgap. Don't build both — YAGNI.

### 6.1 Behaviour

```
./stabilize-cho [--check | --fix] [path]   (default: --check ./cho)

--check  (default):
    For each .cho:
      • already has delimiter?          → report OK, skip
      • clean header, no delimiter?     → report "WILL INSERT" + the line it'd go before
      • anomaly (see §7)?               → report ANOMALY with detail, do NOT auto-fix
    Exit 1 if any file needs work or has an anomaly (CI-friendly).

--fix:
    • Insert the delimiter at the detected boundary for clean files.
    • Idempotent: never inserts a second delimiter.
    • Refuses to auto-fix files flagged as anomalies — prints them for human
      review instead. (Safer to leave a weird file alone than guess.)
```

### 6.2 Idempotency & safety

- Detect an existing delimiter (`^\s*#--PCH:SONG-BODY`) → no-op.
- Git is the safety net: commit before `--fix`; the diff is reviewable
  (one inserted line per file).
- Process per-subtree so we can do it in reviewable batches
  (`./stabilize-cho --fix cho/ABC/` then `cho/DEF/` …).

---

## 7. Anomaly detection (the bonus prize)

Inserting the delimiter forces us to *classify the boundary on every file* —
which is exactly when latent corruption surfaces. The tool should flag (not
auto-fix):

| Anomaly | Why it matters | Example |
|---|---|---|
| **Duplicate `{meta: K …}`** | the original bug | `countin` twice |
| **Deprecated `{meta: version: …}`** | leftover from old format | `{meta: version: 0.0}` |
| **`{meta: K …}` *below* the would-be delimiter** | metadata stranded in body | a meta line after the chords start |
| **Conflicting values for same key** | which is truth? | `backing: RC` + `backing: 32` |
| **Banner/directive mismatch** | echo banner disagrees with real directives | banner says `backing: 32`, directive says `RC` |
| **No recognizable body** | empty/0-byte or malformed file | `Cars/JustWhatINeeded.cho` (0 B, seen earlier) |
| **Multiple banner blocks** | the exact UnderTheMilkyWay shape | two `{comment:**}` fences |
| **Stray `#` remark in header region** | leftover junk, not tool-managed | `FireOnTheMoutain.cho` → `# From: jk13@aol.com` |

This reuses the dupe-scanner logic already prototyped (`/tmp/scan_dupe_meta.py`)
and the existing `ConsistentMetadataService` — fold them together rather than
writing a third copy (DRY).

---

## 8. Interaction with existing tooling

| Tool | Change needed |
|---|---|
| `SongParser` | delimiter-first split, heuristic fallback (§5.2) |
| `SongLineParser` | recognise `#` remark / the sentinel (§5.1) |
| `ChordProFileWriter` | preserve delimiter on round-trip |
| `update-song` / `UpdateSongService` | write banner above delimiter only |
| `assign-backing-track-slots` | rc-slot patch stays in header region |
| `lint-cho.zsh` | optionally add a check: "body delimiter present?" |
| `consistent-metadata` | can assume a clean header region post-migration |
| `consistent-song-data` (future) | body extraction becomes trivial & exact |

---

## 9. Pre-flight facts (verified now)

- **Bare `#` lines DO already exist** (verified) — 3 files use them:
  - `AlanODay/UndercoverAngel.cho` → `#1.`, `#2.`, `#3.` (verse numbering)
  - `AnneMurray/ShadowsInTheMoonlight.cho` → `#1.`–`#5.` (verse numbering)
  - `MarshallTuckerBand/FireOnTheMoutain.cho` → `# From: jk13@aol.com` (a stray
    email header — itself an anomaly worth cleaning).
  → **Therefore the delimiter must match the FULL token** `^\s*#--PCH:SONG-BODY`,
  **never** a bare `^\s*#`. None of the existing `#` lines collide with the
  `--PCH:SONG-BODY` token, so the sentinel is still safe. *(Re-verify the
  specific token at implementation time with `grep -rl '#--PCH:SONG-BODY' cho/`.)*
  → These pre-existing `#` lines are also a reminder that `#` remarks can legally
  appear **in the body** — so the parser must split on the *token*, not on the
  first `#` it sees.
- `EPHEMERAL_COMMENT` (`**`) banner lines are already parsed and **regenerated**
  by the tools — so they're tool-owned and correctly belong above the delimiter.
- At least one **0-byte** file exists (`Cars/JustWhatINeeded.cho`) — the tool
  must handle "no body at all" gracefully (flag as anomaly, don't crash).
- `~640` `.cho` files total across the catalog (the migration's blast radius).

---

## 10. Suggested build order (phased — none of it today)

1. **Phase 0 — confirm the delimiter choice** with Scott (§2 question). Cheap,
   gates everything.
2. **Phase 1 — parser support (read side), behind fallback.** Teach
   `SongParser`/`SongLineParser` the delimiter with heuristic fallback + unit
   tests. *No files change yet.* Fully backward-compatible.
3. **Phase 2 — `--check` mode of `stabilize-cho`.** Read-only audit across the
   whole catalog. Produces the anomaly report (§7). Immediately useful, zero
   risk.
4. **Phase 3 — fix the anomalies** Phase 2 finds (manually or assisted), so the
   catalog is clean *before* we insert delimiters.
5. **Phase 4 — `--fix` insert, in reviewable batches** (per letter-subtree),
   committing as we go (roll-forward/back friendly).
6. **Phase 5 — writers honour the delimiter** + flip `SongParser` to prefer it.
   Optionally add a `lint-cho` check enforcing its presence.

> **Why this order:** detection and parsing land first and are harmless; the
> destructive-ish bulk edit (Phase 4) only happens after the catalog is verified
> clean and well after tomorrow's gig.

---

## 11. Open questions for Scott

1. **Delimiter form** — happy with `#--PCH:SONG-BODY----…`, or want a different
   token/style? (It's invisible in OnSong either way.)
2. **`#` vs `{comment: **}`** — confirm you meant the non-displaying `#` remark
   (§2 question). This is the single biggest assumption in the plan.
3. **Delimiter ownership of blank lines** — should there be exactly one blank
   line above and below the delimiter (tidy, predictable diffs), or preserve
   whatever spacing exists?
4. **Is the delimiter part of the body or neither?** When `SongParser` splits,
   should the delimiter line be (a) dropped from both header & body model but
   preserved on write, or (b) kept as the body's first line? (a) is cleaner.
5. **Tool home** — shell (`stabilize-cho.zsh`, fast) vs picocli command
   (testable, reuses parser)? I lean picocli for the anomaly logic, shell for a
   quick first sweep — but I'd build just one if you want to keep it lean.
6. **Migrate all ~640 at once (in batches) or only as files are touched?** A
   lazy "stabilize on next edit" approach is lower risk but leaves the catalog
   mixed for a while; the parser fallback (§5.2) supports either.

---

## 12. Non-goals (explicitly out of scope here)

- Diffing/normalising song *bodies* across key-variants — that's
  `consistent-song-data.md`, a separate effort.
- Reformatting chords/lyrics or touching anything **below** the delimiter.
- Changing the metadata schema or the catalog CSV.
- Anything at all today. 🎸 (Gig first. Refactors later.)
