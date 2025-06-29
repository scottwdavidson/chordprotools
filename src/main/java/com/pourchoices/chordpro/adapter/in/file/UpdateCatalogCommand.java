package com.pourchoices.chordpro.adapter.in.file;

import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;


@Component
@Command(name = "update-catalog", description = "Updates chord sheets based on the catalog CSV")
public class UpdateCatalogCommand implements Runnable {

    @Parameters(index = "0", description = "Path to the catalog CSV file.")
    private String catalogFile;

    @Override
    public void run() {
        // Your catalog update logic here, using this.catalogFile
        System.out.println("Updating catalog from: " + catalogFile);
        // ... call CatalogUpdaterService with catalogFile ...
    }
}
