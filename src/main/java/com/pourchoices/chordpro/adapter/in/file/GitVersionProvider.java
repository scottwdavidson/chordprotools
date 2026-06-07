package com.pourchoices.chordpro.adapter.in.file;

import picocli.CommandLine.IVersionProvider;

import java.io.InputStream;
import java.util.Properties;

/**
 * Supplies the {@code --version} string for the CLI from the {@code git.properties}
 * file baked into the JAR at build time by the git-commit-id-maven-plugin.
 *
 * <p>The reported version mirrors {@code git describe --tags --dirty --always}:
 * <ul>
 *   <li>on a tagged commit            → {@code v1.2.28}</li>
 *   <li>3 commits past the tag        → {@code v1.2.28-3-g7e0d610}</li>
 *   <li>with uncommitted changes      → {@code v1.2.28-3-g7e0d610-dirty}</li>
 *   <li>no tags yet                   → the abbreviated commit SHA</li>
 * </ul>
 *
 * <p>If {@code git.properties} is missing (e.g. the JAR was built outside a git
 * checkout) it degrades gracefully to {@code "unknown"} rather than throwing.
 */
public class GitVersionProvider implements IVersionProvider {

    @Override
    public String[] getVersion() {
        Properties props = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("git.properties")) {
            if (in == null) {
                return new String[] { "chordpro-tools (version unknown — no git.properties)" };
            }
            props.load(in);
        } catch (Exception e) {
            return new String[] { "chordpro-tools (version unknown)" };
        }

        String describe  = props.getProperty("git.commit.id.describe", "unknown");
        String shortSha  = props.getProperty("git.commit.id.abbrev", "unknown");
        String buildTime = props.getProperty("git.build.time", "unknown");

        return new String[] {
                "chordpro-tools " + describe,
                "commit:  " + shortSha,
                "built:   " + buildTime
        };
    }
}
