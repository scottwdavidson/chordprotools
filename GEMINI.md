# chordprotools — agent bootstrap

You are the working coding agent for **chordprotools**, a Java/Spring Boot CLI
that the Pour Choices band uses to manage ~500 ChordPro (`.cho`) song files and
the metadata that drives live gigs.

## START HERE — read this first, every session

**`KINO_CONTEXT.md`** (repo root) is the canonical, agent-maintained project
brain. Read it in full before doing anything. It contains:

- The CSV schemas (`song-catalog.csv`, `gigs.csv`) and their exact columns
- Every CLI command + its shell-script shim
- The hexagonal (ports & adapters) architecture rules for adding commands
- The RC-500 song-label design rules (12-char hardware display, etc.)
- A dated session-by-session log of completed work and in-progress plans

If anything here and `KINO_CONTEXT.md` disagree, `KINO_CONTEXT.md` wins.

## YOUR JOB re: the context file

`KINO_CONTEXT.md` is a living document. **At the end of any session where you
change code, data schemas, commands, or decisions, update it:**

- Bump the `# Last updated:` line (date + next session number)
- Add a dated entry to the relevant section (Completed / In-progress / etc.)
- Keep it model-agnostic — it's owned by *whatever* agent is working, not by any
  one assistant. (The doc's history mentions an agent named "Kino"; that's just
  prior authorship — you don't have to be Kino to maintain it.)

## QUICK BUILD/RUN FACTS (full detail in KINO_CONTEXT.md)

- Build the fat JAR: `./build`  (auto-picks Maven settings by VPN reachability)
- Run a command fast: `./cpt <command> [args]`  (auto-rebuilds if stale)
- Version check: `./cpt --version`
- After editing any CSV in Sheets/Excel: run `./tidy-song-catalog` or
  `./tidy-gigs` to strip `\r` BEFORE running other commands.
- Tests: `./mvnw test` (JUnit + Mockito).

Welcome aboard. 
