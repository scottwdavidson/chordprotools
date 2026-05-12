package com.pourchoices.chordpro.adapter.out.file;

/**
 * Thrown when an RC-500 {@code .RC0} file cannot be parsed or serialized.
 */
public class Rc500ParseException extends RuntimeException {

    public Rc500ParseException(String message) {
        super(message);
    }

    public Rc500ParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
