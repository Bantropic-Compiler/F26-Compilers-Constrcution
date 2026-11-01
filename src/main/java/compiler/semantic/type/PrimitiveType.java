package compiler.semantic.type;

public final class PrimitiveType extends SemanticType {

    public enum Kind { INTEGER, REAL, BOOLEAN, CHAR }

    public static final PrimitiveType INTEGER = new PrimitiveType(Kind.INTEGER);
    public static final PrimitiveType REAL = new PrimitiveType(Kind.REAL);
    public static final PrimitiveType BOOLEAN = new PrimitiveType(Kind.BOOLEAN);
    public static final PrimitiveType CHAR = new PrimitiveType(Kind.CHAR);

    private final Kind kind;

    private PrimitiveType(Kind kind) {
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    public static PrimitiveType of(Kind kind) {
        return switch (kind) {
            case INTEGER -> INTEGER;
            case REAL -> REAL;
            case BOOLEAN -> BOOLEAN;
            case CHAR -> CHAR;
        };
    }

    @Override
    public String toString() {
        return kind.name().toLowerCase();
    }
}
