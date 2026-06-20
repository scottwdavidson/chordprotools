# ChordPro Style & Best Practices Guide
This document outlines the standard ChordPro structure and our band's specific conventions to ensure chord sheets are clean, highly scannable at "gig speed," and process correctly in OnSong without formatting or rendering errors.

## Core Structure & Directives
Use standard metadata directives at the very top of the file. Keep these minimal to avoid clutter.

```text
{title: Song Title}
{artist: Original Artist}
{key: Am}
{tempo: 120}
{duration: 3:15}
```

## Section Headings & Block Directives
OnSong does not reliably parse abbreviated directives (like {sov} or {soc}). Always spell out directives fully. Furthermore, because OnSong lacks native, reliable support for sections like Bridges, Intros, or Solos via basic headers, we use the explicit _of_part syntax for everything except standard Verses and Choruses.

### Standard Core Blocks
• Verses: Use {start_of_verse} and {end_of_verse}
• Choruses: Use {start_of_chorus} and {end_of_chorus}
### Custom & Instrumental Blocks
For all other structural elements, use the {start_of_part: Name} and {end_of_part} syntax:
• Intro: {start_of_part: Intro} ... {end_of_part}
• Pre-Chorus: {start_of_part: Pre-Chorus} ... {end_of_part}
• Bridge: {start_of_part: Bridge} ... {end_of_part}
• Solo: {start_of_part: Solo} ... {end_of_part}
• Instrumental: {start_of_part: Instrumental} ... {end_of_part}
• Outro: {start_of_part: Outro} ... {end_of_part}

# Instrumental Sections & Measure Blocking
To prevent OnSong from misinterpreting text or forcing an accidental default key context in non-vocal sections, we use an explicit measure grid with a leading period.

### The Rule of the Leading Period
- Always start the line of an instrumental section with a period (.) as the very first character.
- This forces OnSong to treat the line as literal text rather than analyzing it for automatic chord transposition or song key calculations.

### Measure Grid Syntax
- Use vertical pipes | to define the boundaries of a measure.
- Use periods . inside the measure to represent beats where a chord is held or silent.
- Place chords in brackets [X] exactly on the beat they occur.

### Examples:
- One chord held for the full measure (4/4 time): . | [Am] . . . |
- Two measures: Chord A plays for 3 beats, Chord D on beat 4, Chord E holds the next measure: . | [A] . . [D] | [E] . . . |

# Writing Single-Note Licks & Riffs
For quick instrumental hooks, do not use guitar tabs or standard notation. Use one of the two following methods inside an instrumental block depending on complexity.

## Method A: Literal Note Names (Best for Short, Simple Riffs)
For straightforward licks, use lowercase letters for the notes to visually distinguish them from uppercase chord symbols. Space them out or group them by beat to imply the rhythm.

```text
{start_of_part: Intro}
. | [Fm] .    .   [Bb] |
.   Ab C G Ab F G Eb D
{end_of_part}
```

## Method B: Scale Degrees / Nashville Numbers (Best for Complex Riffs or Transposition)
For longer riffs, or songs that the band frequently transposes, use scale degrees relative to the chord root or overall key. This relies on interval muscle memory and remains accurate regardless of what key OnSong shifts the song into.
- Scale context example in F Minor: 1=F, 2=G, 3=Ab, 4=Bb, 5=C, 6=Db, 7=Eb

```text
{start_of_part: Intro}
. | [Fm] . . [Bb] |
.   3 5 2 3  1 2 7 6
{end_of_part}
```

# General Formatting Cheat Sheet
```
{title: Song Title}
{artist: Original Artist}
{key: Am}
{tempo: 120}
{duration: 3:15}

{start_of_part: Intro}
. | [Em] . . . | [C] . . [D] |
.   1 3 5 1
{end_of_part}

{start_of_verse}
[Em] This is how a standard verse line looks
With the [C] chords placed directly [D] over the text.
{end_of_verse}

{start_of_part: Pre-Chorus}
[C] Building up the tension [D] here.
{end_of_part}

{start_of_chorus}
[G] Every chorus line uses [D] the same spacing rules.
{end_of_chorus}

{start_of_part: Bridge}
. | [C] . . . | [D] . . . | [Bm] . . . | [C] . . . |
{end_of_part}

{start_of_part: Outro}
. | [Em] . . . | [C] . . . | [G] . . . | [D] . . . |
{end_of_part}