package com.pourchoices.chordpro.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

/**
 * Configuration for the RC-500 deploy-script generator.
 *
 * <p>Both values default to blank and <em>must</em> be provided either here or
 * via the {@code --source} / {@code --target} CLI options at runtime.
 * They are intentionally left blank in the committed {@code application.properties}
 * because the paths are machine-specific (each band member's library lives somewhere
 * different; the RC-500 mounts at a different path per OS / machine).
 *
 * <p>Override order (highest wins):
 * <ol>
 *   <li>{@code --source} / {@code --target} CLI options</li>
 *   <li>{@code chordprotools.backing-source-root} / {@code chordprotools.rc500-target-root}
 *       in {@code application.properties}</li>
 * </ol>
 */
@Configuration
@PropertySource("classpath:application.properties")
@Component
@Getter
public class ChordproRc500Config {

    /**
     * Root of the local backing-track library.
     * Under this root the directory structure must be:
     * {@code <CLUSTER>/<LETTER>/<Artist>/<SongTitle>/backing.wav}
     * {@code <CLUSTER>/<LETTER>/<Artist>/<SongTitle>/click.wav}
     */
    @Value("${chordprotools.backing-source-root:}")
    private String backingSourceRoot;

    /**
     * Mount point / root of the RC-500 when connected via USB.
     * The generated script will target:
     * {@code <rc500TargetRoot>/ROLAND/WAVE/<NNN>_1/backing.wav}
     * {@code <rc500TargetRoot>/ROLAND/WAVE/<NNN>_2/click.wav}
     */
    @Value("${chordprotools.rc500-target-root:}")
    private String rc500TargetRoot;
}
