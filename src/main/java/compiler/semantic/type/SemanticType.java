package compiler.semantic.type;

public abstract class SemanticType {

    public boolean isError() {
        return this instanceof ErrorType;
    }

    public boolean isVoid() {
        return this instanceof VoidType;
    }

    public boolean isNumeric() {
        return this == PrimitiveType.INTEGER || this == PrimitiveType.REAL;
    }
}
