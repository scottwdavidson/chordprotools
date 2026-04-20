package com.pourchoices.chordpro;

import com.pourchoices.chordpro.adapter.in.file.ChordproToolsMainCommand;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import picocli.CommandLine;

@SpringBootApplication
@Slf4j
public class ChordproParserApplication implements CommandLineRunner {

	private final ChordproToolsMainCommand chordproToolsMainCommand;
	private final CommandLine.IFactory factory;

	public ChordproParserApplication(ChordproToolsMainCommand chordproToolsMainCommand, CommandLine.IFactory factory){
		this.chordproToolsMainCommand = chordproToolsMainCommand;
		this.factory = factory;
	}

	public static void main(String[] args) {

		System.exit(SpringApplication.exit(SpringApplication.run(ChordproParserApplication.class, args)));
	}

	@Override
	public void run(String... args) throws Exception {
		String songsFilename = args[0];

		new CommandLine(chordproToolsMainCommand, factory).execute(args);

	}

}
