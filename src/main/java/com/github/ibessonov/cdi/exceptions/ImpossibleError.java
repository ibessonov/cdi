package com.github.ibessonov.cdi.exceptions;

/**
 * @author ibessonov
 */
public class ImpossibleError extends Error {

    private static final String MESSAGE = "This one mustn't have been happen. Please contact library's developer.";

    public ImpossibleError() {
        super(MESSAGE);
    }

    public ImpossibleError(Throwable cause) {
        super(MESSAGE, cause);
    }

    public ImpossibleError(String message) {
        super(message);
    }
}
