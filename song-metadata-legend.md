# Song Metadata Legend (Rosetta Stone)

For filling in the 5 rating columns in `unrated-songs-to-review.csv` (or
hand-editing `song-catalog.csv` directly). These definitions come straight
from `song-characterization-and-venue-fit.md` §3 — the same rules
`characterize-songs` and `evaluate-setlist` enforce in code.

---

## 1. ENERGY LEVEL — whole number, **1 to 10**

Continuous scale, mellow ballad → dance/upbeat. Rate how the song *feels*
in the mix, not how loud it is.

| Range | Label | Calibration example |
|---|---|---|
| 1–2 | Mellow ballad | *Today's the Day* (America), *Sailing* (Christopher Cross) |
| 3–4 | Medium ballad / soft pop | — |
| 5–6 | Pop / mid-tempo | — |
| 7–8 | Rock | *You Wreck Me*, *Cover Me*, *Bad Case of Lovin' You*, *Mary Jane's Last Dance* |
| 9–10 | Dance / upbeat | — |

Pick a number, not a range — the venue energy-ceiling/floor checks and the
arc-of-the-set math both do direct comparisons.

---

## 2. VOCAL INTENSITY — exactly one of: **`NONE`**, **`LIGHT`**, **`FULL`**

Separate concept from energy — a mellow acoustic song can still have full
lead vocal throughout.

| Value | Meaning |
|---|---|
| `NONE` | Instrumental-friendly; no lead vocal required |
| `LIGHT` | Some/occasional or quiet vocals |
| `FULL` | Full lead vocal throughout |

Must be typed in **ALL CAPS** exactly as shown — it's parsed as an enum, not
free text.

> Heads up: across all 526 already-rated songs, only `FULL` (522) and
> `NONE` (4) actually got used — `LIGHT` exists as a real option but nobody's
> reached for it yet. Don't feel locked into just those two if a song
> genuinely sits in the middle.

---

## 3. GENRE PRIMARY — free text, but **please reuse an existing value**

Descriptive only — never gates or blocks setlist placement today. To keep
it useful for a future "don't stack 3 Country songs in a row" variety
check, stick to the existing vocabulary below rather than inventing a new
label unless nothing fits. Most-used first:

`Rock, Country, Pop, Pop Rock, Soft Rock, Soul, Christmas, Folk Rock, Blues Rock, Alternative, Folk, New Wave, Southern Rock, Synth Pop, Jazz, Blues, Disco, Ballad, Country Rock, Funk, Rockabilly, Latin Rock, Soft Pop, Power Pop, Reggae, Punk, Motown, Rock and Roll, R&B, Roots Rock, Surf Rock, Latin Pop, Dance Pop, Instrumental, Bluegrass, Dream Pop, Glam Rock`

---

## 4. GENRE SECONDARY — free text, **optional**, same rules as above

Leave blank if the primary genre says it all. Existing vocabulary, most-used
first:

`Ballad, Pop, Soul, Pop Rock, New Wave, R&B, Disco, Blues Rock, Folk, Funk, Singer-Songwriter, Surf Rock, Motown, Country, Yacht Rock, Soft Rock, Oldies, Country Rock, Punk, Reggae, Christian, Alternative, Latin Pop, Jazz, Soft Pop, Rock`

---

## 5. SING ALONG — exactly one of: **`true`**, **`false`**

Crowd-engagement marker — does the crowd know the words and want to shout
them back? Independent of energy level; a medium-energy song the crowd
just loves still counts as `true`.

Must be lowercase `true`/`false` exactly (not `TRUE`, `yes`, `1`, etc.) —
parsed as a boolean.

Current split for reference: 129 songs marked `true`, 397 marked `false`.

---

## Quick-fill cheat sheet

| Column | Format | Valid values |
|---|---|---|
| ENERGY LEVEL | integer | `1`–`10` |
| VOCAL INTENSITY | enum, ALL CAPS | `NONE` / `LIGHT` / `FULL` |
| GENRE PRIMARY | free text | prefer list in §3 |
| GENRE SECONDARY | free text, optional | prefer list in §4, or leave blank |
| SING ALONG | boolean, lowercase | `true` / `false` |
