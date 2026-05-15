#!/usr/bin/env zsh
# ==============================================================================
# lint-cho.zsh — ChordPro directive linter / normalizer
#
# Expands shorthand environment directives to their canonical full form and
# fixes known typos so that all .cho files work correctly in OnSong.
#
# Shorthand reference (chordpro.org):
#   {soc}/{eoc} → {start_of_chorus}/{end_of_chorus}
#   {sov}/{eov} → {start_of_verse}/{end_of_verse}
#   {sob}/{eob} → {start_of_bridge}/{end_of_bridge}
#   {sot}/{eot} → {start_of_tab}/{end_of_tab}
#   {sog}/{eog} → {start_of_grid}/{end_of_grid}
# Typo fixes:
#   {start_of_tabs}/{end_of_tabs} → {start_of_tab}/{end_of_tab}
#
# Intentionally left alone:
#   {start_of_part}/{end_of_part} — custom extension, OnSong handles it fine
#   {start_of_grid 4x4+1} etc.   — parameterised form, already full-form
#
# Usage:
#   ./lint-cho.zsh [--check|--fix] [path]
#
#   --check  (default)  scan for violations, report them, exit 1 if any found
#   --fix               apply corrections in-place, exit 0
#   path                .cho file or directory to target (default: ./cho)
#
# Examples:
#   ./lint-cho.zsh                                       # check all of ./cho
#   ./lint-cho.zsh --fix                                 # fix all of ./cho
#   ./lint-cho.zsh --fix cho/STU/                        # fix one subtree
#   ./lint-cho.zsh --check cho/ABC/A/Artist/Song.cho     # check one file
# ==============================================================================

# ── Rules: shorthand / typo → canonical (single source of truth) ─────────────
# To add a rule: append one element to FROM and one to TO, keeping them parallel.
typeset -a FROM TO
FROM=(
  '{soc}'           '{eoc}'
  '{sov}'           '{eov}'
  '{sob}'           '{eob}'
  '{sot}'           '{eot}'
  '{sog}'           '{eog}'
  '{start_of_tabs}' '{end_of_tabs}'
)
TO=(
  '{start_of_chorus}' '{end_of_chorus}'
  '{start_of_verse}'  '{end_of_verse}'
  '{start_of_bridge}' '{end_of_bridge}'
  '{start_of_tab}'    '{end_of_tab}'
  '{start_of_grid}'   '{end_of_grid}'
  '{start_of_tab}'    '{end_of_tab}'
)

# ── Derive grep pattern and perl substitution from the rule arrays ─────────────
# Each FROM element is {word} — strip braces, re-wrap with regex-escaped braces.
# Result examples:
#   GREP_PATTERN  →  \{soc\}|\{eoc\}|...
#   PERL_SUB      →  s/\{soc\}/{start_of_chorus}/g;s/\{eoc\}/{end_of_chorus}/g;...
typeset -a _grep_parts _perl_parts
for i in {1..${#FROM}}; do
  inner="${FROM[$i]:1:-1}"        # strip leading { and trailing }
  from_esc="\\{${inner}\\}"      # wrap with regex-safe \{ and \}
  _grep_parts+=("$from_esc")
  _perl_parts+=("s/${from_esc}/${TO[$i]}/g")
done
readonly GREP_PATTERN="${(j:|:)_grep_parts}"
readonly PERL_SUB="${(j:;:)_perl_parts};"

# ── Argument parsing ───────────────────────────────────────────────────────────
mode="check"
target=""
for arg in "$@"; do
  case "$arg" in
    --check) mode="check" ;;
    --fix)   mode="fix"   ;;
    -*)      print -u2 "lint-cho: unknown option '$arg'"; exit 2 ;;
    *)       target="$arg" ;;
  esac
done
[[ -z "$target" ]] && target="./cho"

# ── File discovery ─────────────────────────────────────────────────────────────
typeset -a FILES
if [[ -f "$target" ]]; then
  FILES=("$target")
elif [[ -d "$target" ]]; then
  FILES=( ${(f)"$(find "$target" -name "*.cho" -type f | sort)"} )
else
  print -u2 "lint-cho: '$target' is not a file or directory"; exit 2
fi

integer total=${#FILES} violations=0 fixed=0

# ── Check mode ─────────────────────────────────────────────────────────────────
if [[ "$mode" == "check" ]]; then
  for f in "${FILES[@]}"; do
    hits=$(grep -nE "$GREP_PATTERN" "$f" 2>/dev/null) || continue
    print "$f"
    while IFS= read -r line; do
      print "  $line"
    done <<< "$hits"
    (( violations++ ))
  done

  print ""
  if (( violations == 0 )); then
    print "✓  All $total file(s) clean — no directive violations."
    exit 0
  else
    print "✗  $violations of $total file(s) have violations.  Run --fix to correct."
    exit 1
  fi
fi

# ── Fix mode ───────────────────────────────────────────────────────────────────
for f in "${FILES[@]}"; do
  count=$(grep -cE "$GREP_PATTERN" "$f" 2>/dev/null) || continue
  perl -i -pe "$PERL_SUB" "$f"
  print "FIXED  $f  ($count line(s) corrected)"
  (( fixed++ ))
done

print ""
print "✓  Fixed $fixed of $total file(s)."
