package com.android.monkeyrunner.exceptions;

/**
 * Base exception class for all MonkeyRunner Exceptions.
 */
public class MonkeyRunnerException extends Exception {
    public MonkeyRunnerException(String message) {
        super(message);
    }

    public MonkeyRunnerException(Throwable e) {
        super(e);
    }

    public MonkeyRunnerException(String message, Throwable e) {
        super(message, e);
    }
}
