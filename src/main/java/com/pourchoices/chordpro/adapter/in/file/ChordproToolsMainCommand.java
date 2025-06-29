package com.pourchoices.chordpro.adapter.in.file;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;

@Component
@Command(name = "chordpro-parser",
		mixinStandardHelpOptions = true,
		subcommands = {
		GenerateIndexCommand.class,
		UpdateCatalogCommand.class,
		UpdateSongCommand.class
})
public class ChordproToolsMainCommand implements Runnable{

	private static final Logger LOGGER = LoggerFactory.getLogger(ChordproToolsMainCommand.class);

	@Override
	public void run() {
		LOGGER.info(" ... should not be here !! ");
		System.out.println("Use one of the subcommands.");
	}

//	@Autowired
//	private HeaderFixer headerFixer;
//
//	public static void main(String[] args) {
//
//		SpringApplication.run(ChordproParserApplication.class, args);
//	}
//
//	@Override
//	public void run(String... args) throws Exception {
//		        String songsFilename = args[0];
//
//        this.headerFixer.fix(songsFilename);
//
//	}
}
