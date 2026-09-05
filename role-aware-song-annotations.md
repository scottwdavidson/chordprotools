# Design: Role-Aware Song Presentation Comments (`stage-songs`)

> **Status:** Captured for later — **not scheduled**. Revisit when a singer
> (or any second on-stage role) actually joins the band.
> **Author:** Kino  · **Date:** 2026-08-31
> **Scope:** Presentation-only annotations that differ per band member, without
> touching the shared song content (lyrics, chords, key).

---

## 1. Problem statement

Today there is exactly one presentation of a song: the `.cho` file itself,
opened by whoever's on stage. If a singer joins the band, two new needs show
up that are explicitly **not** about musical content:

- The singer may want notes ("breathe here", "big note coming", "watch the
  key change") that mean nothing to the guitarist.
- The guitarist may want their own notes ("capo 2, open G shapes") that would
  just be noise to the singer.
- Some notes are genuinely shared and should show up for everyone.

None of this is a key, lyric, or chord difference — which rules out solving
it with another key-variant file (`Song-b.cho`). It's a *presentation* fork of
the same underlying song.

## 2. Rejected approaches (and why)

**A persisted Nashville-number-system "reference song" file.** Considered and
rejected up front: the band only ever needs a base key plus at most one
transposed variant in practice, so a fully relative-key reference file would
be a derived artifact nobody asked for, with its own drift-detection problem
(everything `consistent-song-data` exists to prevent). The Roman-numeral
transform already exists as a pure computation inside `SemanticDiffService`
for drift detection — that's the right amount of "Nashville." If an on-demand
Nashville *view* is ever wanted, it's a cheap read-only export flag later, not
a stored file.

**Sidecar comment files per song** (e.g. `PianoMan.notes.singer.txt`). Initial
instinct, superseded by §3 below once two real OnSong behaviors were
identified. Sidecar files would have worked, but require a separate
anchor-identity scheme (line numbers or named anchors) and a second file to
keep in sync with the reference. Inline annotations solve both problems for
free.

## 3. The two OnSong facts that make this cheap

Scott identified two OnSong rendering behaviors:

1. A line whose **first character is a period (`.`)** is ignored entirely by
   OnSong. (Already leveraged elsewhere for a separate tidying convention —
   out of scope for this doc.)
2. **`{{ <anything> }}`** is treated as an internal/document comment and is
   **never rendered** on stage.

(2) is the key one here. It means an annotation can live **inline, in the
exact spot it applies to**, permanently invisible in OnSong, with zero risk
of ever leaking onto someone's screen — including the reference `.cho` file
itself, opened directly, today, before any staging step exists.

## 4. Existing groundwork already in the codebase

This is the pleasant surprise: `{{ ... }}` is **already a recognized,
tested** construct in the parser — it's just never been wired into
production behavior.

- `SongDirective.DOCUMENT_COMMENT` (`SongDirective.java`) already matches
  `{{ ... }}` and captures the inner text.
- `SongLineParser.parseSongPhrase(line)` already returns a
  `ParsedSongPhrase` with `songDirective = DOCUMENT_COMMENT` and the raw
  inner text as `.line(...)`.
- Covered by `SongLineParserTest` today.
- **Not called from anywhere in production code** — `SongParser.parse()`
  still treats the song body as opaque `List<String>` lines; nothing
  currently calls `parseSongPhrase` outside of tests.

So the body-level "is this line a hidden document comment" primitive already
exists, tested, for free. What's missing is (a) a lightweight convention for
tagging *which role* a document comment belongs to, and (b) a command that
materializes the right ones per role.

## 5. Proposed syntax

Reuse `{{ ... }}` as-is; layer a plain-text role tag inside the payload that
`stage-songs` parses itself — no change needed to `SongDirective` or the
core parser model:

```
{{ singer: Breathe here before the big note }}
{{ guitar: Capo 2, use open G shapes here }}
{{ shared: Watch the key change on beat 3 }}
```

- Tag vocabulary is an open string, not a hardcoded enum — `shared`/`all` as
  a reserved keyword meaning "every role", everything else is a role name
  matched against `--role` at staging time. No schema change needed to add a
  third role (bassist? keys?) later.
- A `{{ ... }}` line with no recognized `tag:` prefix is left exactly as-is
  (untouched, still invisible) — forwards-compatible with any other use of
  document comments that predates this feature.

## 6. What `stage-songs` would do

A new, proper Java command — not logic bolted onto a shell script —
consistent with keeping business logic out of the helper scripts:

```
adapter/in/file/StageSongsCommand.java        <- picocli @Command (--gig, --role, --output)
        |
        v
application/port/in/StageSongsUseCase.java     <- interface
        |
        v
application/domain/service/StageSongsService.java
        |  resolves gig's setlist (reuses SetlistJoiner, same as export-setlist)
        |  for each song: reads the reference .cho, rewrites body lines, writes staged copy
        v
application/domain/service/RoleCommentWeaver.java   <- new, isolated, easily unit-testable
```

Per-line behavior for a given `--role`:

| Line | Action |
|---|---|
| Not a `{{ ... }}` document comment | Copied verbatim (the vast majority of every file) |
| `{{ shared: ... }}` or `{{ all: ... }}` | Materialized into a visible `{comment: ...}` line |
| `{{ <requested-role>: ... }}` | Materialized into a visible `{comment: ...}` line |
| `{{ <other-role>: ... }}` | Dropped entirely from this role's staged copy |
| `{{ ... }}` with no recognized tag | Left untouched (still invisible, forwards-compatible) |

**Songs with zero `{{ }}` lines produce byte-identical output to today's
`copyChoSetlist`.** This is the important backward-compatibility property:
until comments actually get added, staging behaves exactly like it does
today. No regression risk for the no-singer-yet, single-presentation case.

## 7. Anchor identity is free

Because the annotation lives at the exact line it applies to, there's no
separate anchor-name registry to invent or keep in sync (the sidecar-file
approach would have needed one). Position in the file *is* the anchor. It's
naturally robust to edits elsewhere in the song, and if the annotated line
itself is later deleted, the comment just becomes a slightly displaced (but
still present and human-reviewable) note — never a silent loss, never a
crash.

## 8. Relationship to existing staging scripts

`copyChoSetlist` / `copyAllSetlist` / `copyPdfSetlist` / `copySetlist`
currently do a flat, gig-unaware copy into `./work/setlist-ff/`. `stage-songs`
is a **separate, additive** command — not a replacement in this pass. Once
it exists and is trusted, consolidating those scripts into it is a fair
follow-up, but that's a separate migration and explicitly out of scope here.

## 9. Open questions (deferred, not resolved)

1. **Default `--role` behavior.** If `stage-songs` is run with no `--role`,
   should it (a) behave like today — materialize everything as if there's
   one universal role, or (b) strip all tagged comments as the safest
   no-op default? Leaning (a) for backward compatibility, but worth
   deciding against real usage, not speculation.
2. **Does `{{ }}` actually round-trip cleanly through OnSong in practice
   once a file has several of them, including near section markers
   (`{start_of_chorus}` etc.)?** Scott's stated OnSong behavior is almost
   certainly reliable (already relied on for another convention), but worth
   a quick real-file smoke test before writing the weaving logic, not
   after.
3. **Exact role vocabulary** — `guitar` / `singer` is the anticipated set;
   don't lock anything in until there's a real second band member to design
   against.
4. Whether `stage-songs` output should physically replace files already
   staged by the older copy scripts, or stage-songs becomes the one true
   staging command once trusted.

## 10. Why this is low-risk whenever it's picked up

- The hard primitive (`{{ }}` → `DOCUMENT_COMMENT` recognition) is already
  built and tested — zero net-new parsing risk.
- Purely additive: the reference `.cho` files, `song-catalog.csv`, and
  `gigs.csv` are untouched by this feature. `stage-songs` only ever *reads*
  the catalog/gig data and *writes* into a separate staging directory.
- Zero behavior change for any song with no `{{ }}` annotations — which is
  every song in the catalog today.
