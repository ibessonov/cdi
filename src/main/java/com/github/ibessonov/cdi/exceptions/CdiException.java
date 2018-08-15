package com.github.ibessonov.cdi.exceptions;

import com.github.ibessonov.cdi.enums.CdiErrorType;

/**
 * @author ibessonov
 */
public class CdiException extends RuntimeException {

    private final CdiErrorType type;

    public CdiException(Throwable cause, CdiErrorType type, Object... args) {
        super(String.format(type.toString(), args), cause);
        this.type = type;
    }

    public CdiException(CdiErrorType type, Object... args) {
        super(type.toString(args));
        this.type = type;
    }

    public CdiException(String message) {
        super(message);
        this.type = null;
    }

    public CdiErrorType getType() {
        return type;
    }
}
