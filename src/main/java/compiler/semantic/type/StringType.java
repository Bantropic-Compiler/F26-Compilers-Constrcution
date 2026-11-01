package compiler.semantic.type;

public final class StringType extends SemanticType {

    public static final StringType INSTANCE = new StringType();

    private StringType() {
    }

    @Override
    public String toString() {
        return "string";
    }
}
