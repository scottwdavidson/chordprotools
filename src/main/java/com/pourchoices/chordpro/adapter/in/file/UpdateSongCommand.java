package com.pourchoices.chordpro.adapter.in.file;

import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;


@Component
@Command(name = "update-song", description = "Updates a specific chord sheet based on the catalog")
public class UpdateSongCommand implements Runnable {

    @Parameters(index = "0", description = "Path to the catalog CSV file.")
    private String catalogFile;

    @Parameters(index = "1", description = "Key of the chord sheet to update.")
    private String sheetKey;

    @Override
    public void run() {
        // Your single sheet update logic here, using this.catalogFile and this.sheetKey
        System.out.println("Updating sheet '" + sheetKey + "' from: " + catalogFile);
        // ... call SheetUpdaterService with catalogFile and sheetKey ...
    }
}