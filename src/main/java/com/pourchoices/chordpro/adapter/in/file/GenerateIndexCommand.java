package com.pourchoices.chordpro.adapter.in.file;

import com.pourchoices.chordpro.domain.model.HeaderFixer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Component
@Command(name = "generate-index", description = "Generates the chord sheet index")
public class GenerateIndexCommand implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateIndexCommand.class);

    @Parameters(index = "0", description = "Path to the input file listing chord sheet filenames.")
    private String inputFile;

    @Override
    public void run() {

        String filePath = "/Users/scott/Downloads/touch.txt"; // Specify the desired file path

        try {
            File file = new File(filePath);

            if (file.createNewFile()) {
                System.out.println("File created: " + file.getAbsolutePath());
            } else {
                System.out.println("File already exists: " + file.getAbsolutePath());
            }

        } catch (IOException e) {
            System.err.println("Error creating file: " + e.getMessage());
            e.printStackTrace();
        }

        // Your index generation logic here, using this.inputFile
        LOGGER.info("Generating index from: " + inputFile);
        LOGGER.info("Generating index from: " + inputFile);
        LOGGER.info("Generating index from: " + inputFile);
        LOGGER.info("Generating index from: " + inputFile);
        LOGGER.info("Generating index from: " + inputFile);
        LOGGER.info("Generating index from: " + inputFile);
        LOGGER.info("Generating index from: " + inputFile);
        LOGGER.info("Generating index from: " + inputFile);
        LOGGER.info("Generating index from: " + inputFile);
        // ... call IndexGeneratorService with inputFile ...
    }
}