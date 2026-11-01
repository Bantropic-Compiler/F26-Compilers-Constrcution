package compiler.semantic.symbol;

import compiler.ast.Node;
import compiler.semantic.type.SemanticType;

public final class TypeSymbol extends Symbol {

    private SemanticType type;

    public TypeSymbol(String name, Node declaration) {
        super(name, declaration);
    }

    public SemanticType type() {
        return type;
    }

    public void setType(SemanticType type) {
        this.type = type;
    }
}
