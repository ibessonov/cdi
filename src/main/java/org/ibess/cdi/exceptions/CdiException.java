package org.ibess.cdi.exceptions;

import org.ibess.cdi.enums.CdiErrorType;

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

    public CdiErrorType getType() {
        return type;
    }
}
