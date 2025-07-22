package com.pourchoices.chordpro.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

/**
 * Configuration class for ChordProTools properties.
 * This class reads the 'chordprotools.catalog-index' property from the application's
 * configuration sources (e.g., application.properties, application.yml).
 */
@Configuration // Marks this class as a source of bean definitions and configuration
@PropertySource("classpath:application.properties") // Specifies the property file to load
@Component // Also mark as a component so it can be picked up by component scanning
@Getter
public class ChordproCatalogIndexPathConfig {

    @Value("${chordprotools.catalog-index:}") // ":"" provides a default empty string if property is not found
    private String catalogIndexPath;

}
