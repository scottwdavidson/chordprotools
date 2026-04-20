package com.pourchoices.chordpro.adapter.in.file;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;

@Component
@Command(name = "chordpro-parser",
        mixinStandardHelpOptions = true,
        subcommands = {
                GenerateSongCatalogCommand.class,
                ImportNewSongCommand.class,
                UpdateSongsCommand.class,
                UpdateSongCommand.class
        })
@Slf4j
public class ChordproToolsMainCommand implements Runnable {


    @Override
    public void run() {
        log.info(" ... should not be here !! ");
        System.out.println("Use one of the subcommands.");
    }

}
