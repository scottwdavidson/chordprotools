package com.pourchoices.chordpro.adapter.in.file;

import com.pourchoices.chordpro.application.port.in.GenerateRc500DeployScriptUseCase;
import com.pourchoices.chordpro.config.ChordproRc500Config;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * CLI command that generates a timestamped shell script for copying RC-500
 * backing and click tracks from the local library to the pedal.
 *
 * <p>The generated script contains only plain {@code cp} commands (no
 * {@code mkdir} or other logic) so it can be inspected, trimmed, and run
 * exactly as needed — for example, copying only a subset of songs ahead of a
 * practice session.
 *
 * <p>Usage:
 * <pre>
 *   # Generate for the latest gig, using application.properties defaults:
 *   chordpro-tools deploy-rc500
 *
 *   # Specific gig, override paths at the command line:
 *   chordpro-tools deploy-rc500 --gig 2026-06-14-rusty-nail \
 *       --source /Volumes/G-DRIVE/BackingTracks \
 *       --target /Volumes/RC-500
 *
 *   # Write the generated script to a specific directory:
 *   chordpro-tools deploy-rc500 --output-dir ~/Desktop
 * </pre>
 */
@Component
@Command(
        name = "deploy-rc500",
        description = "Generates a timestamped shell script (deploy-rc500-<timestamp>.sh) " +
                      "containing cp commands to copy backing and click tracks to the RC-500."
)
@Slf4j
public class GenerateRc500DeployScriptCommand implements Runnable {

    private final GenerateRc500DeployScriptUseCase useCase;
    private final ChordproRc500Config              rc500Config;

    public GenerateRc500DeployScriptCommand(GenerateRc500DeployScriptUseCase useCase,
                                            ChordproRc500Config rc500Config) {
        this.useCase     = useCase;
        this.rc500Config = rc500Config;
    }

    @Option(
            names = {"--gig", "-g"},
            description = "Gig slug (e.g. 2026-06-14-rusty-nail). " +
                          "Defaults to the lexicographically latest gig in gigs.csv."
    )
    private String gig;

    @Option(
            names = {"--source", "-s"},
            description = "Root of the local backing-track library. " +
                          "Overrides chordprotools.backing-source-root in application.properties."
    )
    private String source;

    @Option(
            names = {"--target", "-t"},
            description = "RC-500 mount point / root directory. " +
                          "Overrides chordprotools.rc500-target-root in application.properties."
    )
    private String target;

    @Option(
            names = {"--output-dir", "-o"},
            description = "Directory in which to write the generated script (default: current directory).",
            defaultValue = "."
    )
    private String outputDir;

    @Override
    public void run() {
        String resolvedSource = resolve("--source / backing-source-root", source, rc500Config.getBackingSourceRoot());
        String resolvedTarget = resolve("--target / rc500-target-root",   target, rc500Config.getRc500TargetRoot());

        Path outputDirPath = Paths.get(outputDir).toAbsolutePath();

        log.info("Generating RC-500 deploy script: gig={}, source={}, target={}, outputDir={}",
                gig, resolvedSource, resolvedTarget, outputDirPath);

        Path generated = useCase.generateDeployScript(gig, resolvedSource, resolvedTarget, outputDirPath);

        System.out.printf("%nRC-500 deploy script generated:%n  %s%n%n", generated);
        System.out.println("Review and edit the script, then run it:");
        System.out.printf("  ./%s%n%n", generated.getFileName());
    }

    /**
     * Returns the CLI option value when provided, falling back to the
     * application-properties value. Throws clearly if neither is set.
     */
    private String resolve(String label, String cliValue, String configValue) {
        if (cliValue != null && !cliValue.isBlank()) return cliValue;
        if (configValue != null && !configValue.isBlank()) return configValue;
        throw new IllegalStateException(
                label + " is not configured. " +
                "Set it via the CLI option or in application.properties.");
    }
}
