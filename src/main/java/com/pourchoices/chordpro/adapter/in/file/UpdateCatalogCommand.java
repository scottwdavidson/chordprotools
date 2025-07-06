package com.pourchoices.chordpro.adapter.in.file;

import com.pourchoices.chordpro.application.domain.port.in.UpdateCatalogUseCase;
import com.pourchoices.chordpro.application.domain.port.in.UpdateSongUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;


@Component
@Command(name = "update-catalog", description = "Updates chord sheets based on the catalog CSV")
public class UpdateCatalogCommand implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateCatalogCommand.class);

    @Parameters(index = "0", description = "Path to the song used to update the catalog.")
    private String chordproSongPathString;

    @Autowired
    private UpdateCatalogUseCase updateCatalogService;

    @Override
    public void run() {

        // Your index generation logic here, using this.inputFile
        LOGGER.info("Updating Catalog from: {}", chordproSongPathString);

        this.updateCatalogService.updateCatalog(chordproSongPathString);
    }
}
