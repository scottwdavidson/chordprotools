package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.CatalogEntry;
import com.pourchoices.chordpro.application.domain.model.ChordProPath;
import com.pourchoices.chordpro.application.domain.model.HeaderDirective;
import com.pourchoices.chordpro.application.domain.model.ParsedHeader;
import com.pourchoices.chordpro.application.domain.model.ParsedHeaderLine;
import com.pourchoices.chordpro.application.domain.model.ParsedSong;
import com.pourchoices.chordpro.application.domain.model.SongId;
import com.pourchoices.chordpro.application.port.in.UpdateSongUseCase;
import com.pourchoices.chordpro.application.port.out.CatalogPort;
import com.pourchoices.chordpro.application.port.out.ChordProPort;
import com.pourchoices.chordpro.config.ChordproCatalogIndexPathConfig;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * Updates a chordpro song file based on the current song catalog metadata.
 *
 * <p>Song metadata (duration, count-in, tempo, …) is shared across all
 * key-variants of a song, so a single {@link #updateSong(SongId)} call fans
 * out to the base file and every key-variant in the same song group.
 */
@Service
@AllArgsConstructor(onConstructor_ = @__(@Autowired))
@Slf4j
public class UpdateSongService implements UpdateSongUseCase {

    private final CatalogPort catalogPort;
    private final ChordproCatalogIndexPathConfig chordproCatalogIndexPathConfig;
    private final ChordProPort chordProPort;
    private final SongParser songParser;
    private final CatalogEntryToParsedHeaderMapper catalogEntryToParsedHeaderMapper;

    @Override
    public void updateSong(SongId songId) {

        // load the catalog (keyed by canonical song-ID string)
        Path catalogPath = Paths.get(chordproCatalogIndexPathConfig.getCatalogIndexPath());
        Map<String, CatalogEntry> catalogMap = this.catalogPort.readCatalogFromCsv(catalogPath);

        // Metadata is a property of the song group, not the individual file:
        // update the base file and every key-variant sharing this group key.
        String groupKey = songId.toGroupKey();
        List<CatalogEntry> groupEntries = catalogMap.values().stream()
                .filter(entry -> entry.getSongId().toGroupKey().equals(groupKey))
                .toList();

        if (groupEntries.isEmpty()) {
            log.error("No catalog entries found for song group '{}' (from song ID: {})",
                    groupKey, songId);
            return;
        }

        groupEntries.forEach(this::updateCatalogEntryFile);
    }

    /**
     * Applies a single catalog entry's metadata to its corresponding
     * {@code .cho} file, preserving any gig-specific RC-slot already present.
     */
    private void updateCatalogEntryFile(CatalogEntry catalogEntry) {

        String chordproSongPathString = ChordProPath.toFilePath(catalogEntry.getSongId());
        log.info("Updating song file: {}", chordproSongPathString);

        // map the catalog entry into a parsed header
        ParsedHeader catalogHeader =
                this.catalogEntryToParsedHeaderMapper.fromCatalogEntry(catalogEntry);

        // parse the current song file
        Path chordproSongPath = Paths.get(chordproSongPathString);
        List<String> chordproFileAsList = this.chordProPort.read(chordproSongPath);
        ParsedSong currentParsedSong = this.songParser.parse(chordproSongPathString, chordproFileAsList);

        // RC_SLOT is a gig-specific assignment, not a catalog property.
        // Preserve whatever slot is currently in the file so that update-song
        // never silently erases a slot written by assign-backing-track-slots.
        ParsedHeader newHeader = withPreservedRcSlot(catalogHeader,
                currentParsedSong.getParsedHeader());

        // replace the header if changed
        if (currentParsedSong.getParsedHeader().compareTo(newHeader) != 0) {
            ParsedSong newSong = currentParsedSong.withHeader(newHeader);
            this.chordProPort.write(chordproSongPath, newSong);
        }
    }

    /**
     * Returns a header identical to {@code catalogHeader} but with the
     * {@link HeaderDirective#RC_SLOT} line from {@code fileHeader} injected,
     * if the file currently has one.
     *
     * <p>This keeps the rc-slot in the {@code .cho} file across ordinary
     * {@code update-song} runs — the slot is owned by {@code gigs.csv} and
     * written directly by {@code assign-backing-track-slots}.
     */
    private ParsedHeader withPreservedRcSlot(ParsedHeader catalogHeader,
                                             ParsedHeader fileHeader) {
        ParsedHeaderLine existingRcSlot = fileHeader.getHeaderLines().stream()
                .filter(l -> l.getHeaderDirective() == HeaderDirective.RC_SLOT)
                .findFirst()
                .orElse(null);

        if (existingRcSlot == null) return catalogHeader;

        ParsedHeader.ParsedHeaderBuilder builder = ParsedHeader.builder()
                .chordProFilename(catalogHeader.getChordProFilename());
        catalogHeader.getHeaderLines().forEach(builder::headerLine);
        builder.headerLine(existingRcSlot);
        return builder.build();
    }
}
