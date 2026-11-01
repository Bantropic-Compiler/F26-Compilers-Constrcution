package compiler.semantic.symbol;

import compiler.ast.Node;

public abstract class Symbol {

    private final String name;
    private final Node declaration;

    protected Symbol(String name, Node declaration) {
        this.name = name;
        this.declaration = declaration;
    }

    public String name() {
        return name;
    }

    public Node declaration() {
        return declaration;
    }
}
