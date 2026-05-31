package com.pourchoices.chordpro.application.port.in;

import java.nio.file.Path;

/**
 * Input port for the RC-500 deploy-script generator.
 *
 * <p>Reads {@code gigs.csv} for the target gig, resolves source and target
 * paths from each song's {@code SongId} and RC slot, then writes a
 * timestamped shell script containing a series of plain {@code cp} commands.
 *
 * <p>The generated script is intentionally simple so the user can inspect and
 * edit it before running — for example, copying only a subset of songs ahead
 * of a practice session.
 *
 * @see com.pourchoices.chordpro.application.domain.service.GenerateRc500DeployScriptService
 */
public interface GenerateRc500DeployScriptUseCase {

    /**
     * Generates the deploy script and returns the path to the written file.
     *
     * @param gigParam         gig slug, or {@code null} / blank to auto-resolve to the latest gig
     * @param backingSourceRoot root of the local backing-track library
     * @param rc500TargetRoot   RC-500 mount point / root directory
     * @param outputDir         directory in which to write the generated script
     * @return path to the generated {@code deploy-rc500-<timestamp>.sh} file
     */
    Path generateDeployScript(String gigParam,
                              String backingSourceRoot,
                              String rc500TargetRoot,
                              Path outputDir);
}
