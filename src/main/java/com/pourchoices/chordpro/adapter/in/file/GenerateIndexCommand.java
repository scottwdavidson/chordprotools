package com.pourchoices.chordpro.adapter.in.file;

import com.pourchoices.chordpro.application.domain.port.in.GenerateIndexUseCase;
import com.pourchoices.chordpro.application.domain.service.GenerateIndexService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Component
@Command(name = "generate-index", description = "Generates the chord sheet index")
public class GenerateIndexCommand implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateIndexCommand.class);

    @Autowired
    private GenerateIndexUseCase generateIndexService;

    @Parameters(index = "0", description = "Path to the songs listing.")
    private String songsListingPathString;

    @Override
    public void run() {

        // Your index generation logic here, using this.inputFile
        LOGGER.info("Generating index from: {}", songsListingPathString);

        this.generateIndexService.generateIndex(songsListingPathString);

    }
}