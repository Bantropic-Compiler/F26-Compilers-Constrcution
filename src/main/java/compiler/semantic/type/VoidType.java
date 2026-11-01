package compiler.semantic.type;

public final class VoidType extends SemanticType {

    public static final VoidType INSTANCE = new VoidType();

    private VoidType() {
    }

    @Override
    public String toString() {
        return "void";
    }
}
