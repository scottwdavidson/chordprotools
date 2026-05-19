package com.pourchoices.chordpro.application.domain.model;

import lombok.Builder;
import lombok.Value;

/**
 * A joined view of a {@link CatalogEntry} (song metadata) and a {@link SetlistAssignment}
 * (gig position). This is the unit of currency for all setlist-producing services.
 *
 * <p>Convenience delegate methods are provided for the fields most commonly accessed
 * by setlist services and CLI commands, so callers rarely need to drill into
 * {@link #getSong()} or {@link #getAssignment()} directly.
 */
@Value
@Builder
public class SetlistEntry {

    /** Full song metadata from {@code song-catalog.csv}. */
    CatalogEntry song;

    /** Gig assignment from {@code setlist-assignments.csv}. */
    SetlistAssignment assignment;

    // ── Delegates: assignment ────────────────────────────────────────────

    /** Compound set-position code, e.g. {@code A01}. Sourced from the assignment. */
    public String getSet()    { return assignment.getSet(); }

    /** Gig identifier slug, e.g. {@code 2026-06-14-rusty-nail}. */
    public String getGig()    { return assignment.getGig(); }

    // ── Delegates: song ──────────────────────────────────────────────────

    public SongId getSongId()           { return song.getSongId(); }
    public String getTitle()            { return song.getTitle(); }
    public String getArtist()           { return song.getArtist(); }
    public String getKey()              { return song.getKey(); }
    /** RC-500 slot number for display in the setlist; null for BB and no-backing songs. */
    public String getBacking()          { return song.getRcSlot(); }
    public BackingType getBackingType() { return song.getBackingType(); }
    public String getPerformanceKey()   { return song.getPerformanceKey(); }
}
