package com.pourchoices.chordpro.adapter.in.file;

import picocli.CommandLine;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.ParameterException;

public class CustomPicocliExceptionHandler implements CommandLine.IParameterExceptionHandler {

    @Override
    public int handleParseException(ParameterException ex, String[] args) throws Exception {
        // Handle parsing exceptions (including UnmatchedArgumentException)
        if (ex instanceof CommandLine.UnmatchedArgumentException) {
            System.err.println("Error: Unrecognized command or arguments.");
            CommandLine commandLine = ex.getCommandLine();
            commandLine.usage(commandLine.getErr(), Ansi.AUTO); // Show usage help
            return commandLine.getCommandSpec().exitCodeOnInvalidInput();
        } else {
            System.err.println("Error parsing command line: " + ex.getMessage());
            CommandLine commandLine = ex.getCommandLine();
            commandLine.usage(commandLine.getErr(), Ansi.AUTO); // Show usage help
            return commandLine.getCommandSpec().exitCodeOnInvalidInput();
        }
    }
}
