package compiler.semantic;

import compiler.ast.Node;
import compiler.semantic.symbol.RoutineSymbol;
import compiler.semantic.symbol.Scope;
import compiler.semantic.symbol.Symbol;
import compiler.semantic.symbol.TypeSymbol;
import compiler.semantic.symbol.VariableSymbol;
import compiler.semantic.type.ErrorType;
import compiler.semantic.type.SemanticType;
import java.util.IdentityHashMap;
import java.util.Map;

public final class SemanticModel {

    private final Map<Node, SemanticType> types = new IdentityHashMap<>();
    private final Map<Node, Symbol> symbols = new IdentityHashMap<>();
    private final Map<Node, Scope> scopes = new IdentityHashMap<>();
    private final Scope globalScope = new Scope(null);

    public Scope globalScope() {
        return globalScope;
    }

    public void setType(Node node, SemanticType type) {
        types.put(node, type);
    }

    public SemanticType typeOf(Node node) {
        SemanticType type = types.get(node);
        return type != null ? type : ErrorType.INSTANCE;
    }

    public boolean hasType(Node node) {
        return types.containsKey(node);
    }

    public void bind(Node node, Symbol symbol) {
        symbols.put(node, symbol);
    }

    public Symbol symbolOf(Node node) {
        return symbols.get(node);
    }

    public VariableSymbol variableOf(Node node) {
        return symbols.get(node) instanceof VariableSymbol symbol ? symbol : null;
    }

    public RoutineSymbol routineOf(Node node) {
        return symbols.get(node) instanceof RoutineSymbol symbol ? symbol : null;
    }

    public TypeSymbol typeSymbolOf(Node node) {
        return symbols.get(node) instanceof TypeSymbol symbol ? symbol : null;
    }

    public void setScope(Node node, Scope scope) {
        scopes.put(node, scope);
    }

    public Scope scopeOf(Node node) {
        return scopes.get(node);
    }
}
