package compiler.semantic.type;

public final class ErrorType extends SemanticType {

    public static final ErrorType INSTANCE = new ErrorType();

    private ErrorType() {
    }

    @Override
    public String toString() {
        return "<error>";
    }
}
