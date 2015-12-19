package ibessonov.cdi.enums;

/**
 * @author ibessonov
 */
public enum CdiErrorType {

    GENERIC_PARAMETERS_COUNT_MISMATCH("Generic parameters count mismatch for class %s. Expected: %d. Actual: %d"),
    PRIMITIVE_TYPE_LOOKUP("Attempt to lookup object with primitive type: %s"),
    ILLEGAL_ACCESS("Illegal access"),
    TOO_MANY_CONSTRUCTORS("Invalid @Scoped class %s. More than one method annotated with @Constructor has been found"),
    CLASS_INJECTION("Invalid @Scoped class %s. Cannot inject field with type %s. Class injection is available for generic class parameter types only"),
    FINAL_SCOPED_CLASS("Invalid @Scoped class %s. 'final' modifier is not allowed"),
    PARAMETERIZED_NON_STATELESS("Invalid @Scoped class %s. Generic class has to be STATELESS"),
    UNIMPLEMENTABLE_ABSTRACT_METHOD("Invalid @Scoped class %s. Abstract method '%s' must have 0 parameters and @Provided annotation. Return type should also be valid for injection");

    private final String format;

    CdiErrorType(String format) {
        this.format = format;
    }

    @Override
    public String toString() {
        return format;
    }
}
