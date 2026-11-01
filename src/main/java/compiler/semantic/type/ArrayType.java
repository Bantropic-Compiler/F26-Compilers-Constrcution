package compiler.semantic.type;

public final class ArrayType extends SemanticType {

    public static final long NO_SIZE = -1;

    private final SemanticType elementType;
    private final long size;

    public ArrayType(SemanticType elementType, long size) {
        this.elementType = elementType;
        this.size = size;
    }

    public SemanticType elementType() {
        return elementType;
    }

    public boolean hasSize() {
        return size != NO_SIZE;
    }

    public long size() {
        return size;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ArrayType that
                && size == that.size
                && elementType.equals(that.elementType);
    }

    @Override
    public int hashCode() {
        return 31 * elementType.hashCode() + Long.hashCode(size);
    }

    @Override
    public String toString() {
        return "array[" + (hasSize() ? String.valueOf(size) : "") + "] " + elementType;
    }
}
