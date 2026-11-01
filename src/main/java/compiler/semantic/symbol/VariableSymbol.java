package compiler.semantic.symbol;

import compiler.ast.Node;
import compiler.semantic.type.SemanticType;

public final class VariableSymbol extends Symbol {

    public enum Kind { GLOBAL, LOCAL, PARAMETER, LOOP, FIELD }

    private final Kind kind;
    private SemanticType type;

    public VariableSymbol(String name, Node declaration, Kind kind) {
        super(name, declaration);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    public SemanticType type() {
        return type;
    }

    public void setType(SemanticType type) {
        this.type = type;
    }
}
