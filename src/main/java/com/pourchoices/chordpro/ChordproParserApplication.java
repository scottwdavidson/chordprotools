package com.pourchoices.chordpro;

import com.pourchoices.chordpro.domain.model.HeaderFixer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ChordproParserApplication implements CommandLineRunner {

	@Autowired
	private HeaderFixer headerFixer;

	public static void main(String[] args) {

		SpringApplication.run(ChordproParserApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		        String songFilename = args[0];

        this.headerFixer.fix(songFilename);

	}
}
