package com.pourchoices.chordpro.application.domain.service;

import com.pourchoices.chordpro.application.domain.model.SetlistAssignment;
import com.pourchoices.chordpro.application.domain.model.SongId;
import com.pourchoices.chordpro.application.port.in.GenerateRc500DeployScriptUseCase;
import com.pourchoices.chordpro.application.port.out.SetlistAssignmentsPort;
import com.pourchoices.chordpro.config.ChordproGigsPathConfig;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

/**
 * Generates a timestamped, human-editable shell script that copies backing
 * and click WAV files from a local library to the RC-500 looper pedal.
 *
 * <h3>Source path convention</h3>
 * <pre>
 *   &lt;backingSourceRoot&gt;/&lt;CLUSTER&gt;/&lt;LETTER&gt;/&lt;Artist&gt;/&lt;SongTitle&gt;/backing.wav
 *   &lt;backingSourceRoot&gt;/&lt;CLUSTER&gt;/&lt;LETTER&gt;/&lt;Artist&gt;/&lt;SongTitle&gt;/click.wav
 * </pre>
 * {@code SongTitle} is always the base title (key-variant suffix stripped) because
 * the backing-track library is organised by song, not by key.
 * {@link SongId} already exposes this cleanly via {@link SongId#getTitle()}.
 *
 * <h3>Target path convention (standard RC-500 WAVE structure)</h3>
 * <pre>
 *   &lt;rc500TargetRoot&gt;/ROLAND/WAVE/&lt;NNN&gt;_1/backing.wav
 *   &lt;rc500TargetRoot&gt;/ROLAND/WAVE/&lt;NNN&gt;_2/click.wav
 * </pre>
 * {@code NNN} is the RC slot number zero-padded to three digits (no offset —
 * slot 7 → {@code 007}, slot 50 → {@code 050}).
 *
 * <h3>Generated script rules</h3>
 * <ul>
 *   <li>Pure {@code cp} commands only — no {@code mkdir -p}, no other logic.</li>
 *   <li>Missing {@code backing.wav} at generation time → {@code cp} command is
 *       commented out with a conspicuous WARNING header; the user must fix the
 *       source library before uncommenting.</li>
 *   <li>Missing {@code click.wav} at generation time → {@code cp} command is
 *       omitted entirely with an INFO comment (no click track is normal for some songs).</li>
 *   <li>Songs without an RC slot assigned are skipped silently (they belong to the
 *       gig but don't use RC-500 backing tracks).</li>
 * </ul>
 */
@Service
@AllArgsConstructor(onConstructor_ = @__(@Autowired))
@Slf4j
public class GenerateRc500DeployScriptService implements GenerateRc500DeployScriptUseCase {

    private static final String BACKING_FILE = "backing.wav";
    private static final String CLICK_FILE   = "click.wav";
    private static final String WAVE_SUBDIR  = "ROLAND/WAVE";

    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final DateTimeFormatter HEADER_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final SetlistAssignmentsPort assignmentsPort;
    private final SetlistJoiner          setlistJoiner;
    private final ChordproGigsPathConfig gigsConfig;

    @Override
    public Path generateDeployScript(String gigParam,
                                     String backingSourceRoot,
                                     String rc500TargetRoot,
                                     Path outputDir) {

        // ── 1. Load assignments and resolve gig ───────────────────────────────
        List<SetlistAssignment> allAssignments =
                assignmentsPort.readAssignments(Paths.get(gigsConfig.getGigsPath()));

        String resolvedGig = setlistJoiner.resolveGig(gigParam, allAssignments);
        if (resolvedGig == null) {
            throw new IllegalStateException("No gig assignments found in gigs.csv");
        }
        log.info("Generating RC-500 deploy script for gig: {}", resolvedGig);

        // ── 2. Filter to this gig's RC-slotted assignments, ordered by slot ───
        List<SetlistAssignment> rcAssignments = allAssignments.stream()
                .filter(a -> resolvedGig.equals(a.getGig()))
                .filter(a -> a.getRcSlot() != null && !a.getRcSlot().isBlank())
                .sorted(Comparator.comparingInt(a -> Integer.parseInt(a.getRcSlot().trim())))
                .toList();

        log.info("Found {} RC-slotted assignment(s) for gig '{}'",
                rcAssignments.size(), resolvedGig);

        // ── 3. Build and write the script ──────────────────────────────────────
        LocalDateTime now       = LocalDateTime.now();
        String        timestamp = now.format(TIMESTAMP_FMT);
        String        scriptName = "deploy-rc500-" + timestamp + ".sh";
        Path          scriptPath = outputDir.resolve(scriptName);

        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(scriptPath))) {
            writeHeader(out, resolvedGig, backingSourceRoot, rc500TargetRoot,
                        now, scriptName, rcAssignments.size());

            int copied  = 0;
            int warned  = 0;

            for (SetlistAssignment assignment : rcAssignments) {
                SongId songId    = assignment.getSongId();
                String slotPadded = String.format("%03d", Integer.parseInt(assignment.getRcSlot().trim()));

                // Source: always use base title (SongId.getTitle() has key-variant already stripped)
                Path srcDir = Paths.get(backingSourceRoot,
                        songId.getClusterPrefix(),
                        songId.getClusterElement(),
                        songId.getArtist(),
                        songId.getTitle());

                Path srcBacking = srcDir.resolve(BACKING_FILE);
                Path srcClick   = srcDir.resolve(CLICK_FILE);

                // Target directories (WAVE structure)
                String tgtBacking = rc500TargetRoot + "/" + WAVE_SUBDIR + "/" + slotPadded + "_1/" + BACKING_FILE;
                String tgtClick   = rc500TargetRoot + "/" + WAVE_SUBDIR + "/" + slotPadded + "_2/" + CLICK_FILE;

                // Song section header comment
                String label = songId.getArtist() + " / " + songId.getTitle();
                out.printf("%n# ── %s  [slot %s / set %s] %s%n",
                        label, slotPadded, assignment.getSet(),
                        "─".repeat(Math.max(0, 54 - label.length())));

                // backing.wav
                if (Files.exists(srcBacking)) {
                    out.printf("cp \"%s\" \\\n   \"%s\"%n", srcBacking, tgtBacking);
                    copied++;
                } else {
                    log.warn("backing.wav NOT FOUND for {} — cp will be commented out in script", label);
                    warned++;
                    out.printf("# ⚠ WARNING: backing.wav NOT FOUND at generation time%n");
                    out.printf("# Expected : %s%n", srcBacking);
                    out.printf("# Uncomment once the file is in place:%n");
                    out.printf("# cp \"%s\" \\%n#    \"%s\"%n", srcBacking, tgtBacking);
                }

                // click.wav (optional)
                if (Files.exists(srcClick)) {
                    out.printf("cp \"%s\" \\\n   \"%s\"%n", srcClick, tgtClick);
                } else {
                    out.printf("# INFO: No click.wav found for %s — omitted%n", label);
                }
            }

            writeFooter(out, resolvedGig, rcAssignments.size(), copied, warned);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write deploy script: " + scriptPath, e);
        }

        // Make the script executable
        makeExecutable(scriptPath);

        log.info("Deploy script written: {}", scriptPath);
        return scriptPath;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void writeHeader(PrintWriter out, String gig, String source, String target,
                             LocalDateTime now, String scriptName, int songCount) {
        out.println("#!/bin/zsh");
        out.println("# " + "=".repeat(63));
        out.println("# RC-500 Deploy Script");
        out.printf( "# Generated : %s%n", now.format(HEADER_FMT));
        out.printf( "# Gig       : %s%n", gig);
        out.printf( "# Songs     : %d RC-slotted assignment(s)%n", songCount);
        out.printf( "# Source    : %s%n", source);
        out.printf( "# Target    : %s%n", target);
        out.println("# " + "=".repeat(63));
        out.println("# Edit before running — copy only the songs you need.");
        out.printf( "# Run: ./%s%n", scriptName);
        out.println("# " + "=".repeat(63));
    }

    private void writeFooter(PrintWriter out, String gig, int total, int backing, int warned) {
        out.println();
        out.println("# " + "=".repeat(63));
        out.printf( "# End of deploy script for gig: %s%n", gig);
        out.printf( "# Total RC-slotted songs : %d%n", total);
        out.printf( "# backing.wav found      : %d%n", backing);
        if (warned > 0) {
            out.printf("# ⚠ backing.wav MISSING  : %d  ← search for WARNING above%n", warned);
        }
        out.println("# " + "=".repeat(63));
    }

    private void makeExecutable(Path scriptPath) {
        try {
            Files.setPosixFilePermissions(scriptPath,
                    PosixFilePermissions.fromString("rwxr-xr-x"));
        } catch (UnsupportedOperationException e) {
            log.debug("POSIX permissions not supported on this OS — skipping chmod for {}", scriptPath);
        } catch (IOException e) {
            log.warn("Could not set executable permission on {}: {}", scriptPath, e.getMessage());
        }
    }
}
