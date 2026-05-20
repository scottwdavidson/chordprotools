package com.pourchoices.chordpro.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

/**
 * Resolves the filesystem path for {@code gigs.csv}.
 */
@Configuration
@PropertySource("classpath:application.properties")
@Component
@Getter
public class ChordproGigsPathConfig {

    @Value("${chordprotools.gigs:./gigs.csv}")
    private String gigsPath;
}
