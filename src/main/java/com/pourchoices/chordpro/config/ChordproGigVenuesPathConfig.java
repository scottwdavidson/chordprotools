package com.pourchoices.chordpro.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

/**
 * Resolves the filesystem path for {@code gig-venues.csv}.
 */
@Configuration
@PropertySource("classpath:application.properties")
@Component
@Getter
public class ChordproGigVenuesPathConfig {

    @Value("${chordprotools.gig-venues:./gig-venues.csv}")
    private String gigVenuesPath;
}
