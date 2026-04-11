package com.bitirme.demo_bitirme.exception;

public class ExifExtractionException extends RuntimeException {
    public ExifExtractionException(String message) {
        super(message);
    }

    public ExifExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}
