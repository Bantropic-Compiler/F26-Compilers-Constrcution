package compiler.semantic.symbol;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Scope {

    private final Scope parent;
    private final Map<String, Symbol> symbols = new LinkedHashMap<>();

    public Scope(Scope parent) {
        this.parent = parent;
    }

    public Scope parent() {
        return parent;
    }

    public boolean isGlobal() {
        return parent == null;
    }

    public void declare(Symbol symbol) {
        symbols.put(symbol.name(), symbol);
    }

    public Symbol lookupLocal(String name) {
        return symbols.get(name);
    }

    public Symbol lookup(String name) {
        for (Scope scope = this; scope != null; scope = scope.parent) {
            Symbol symbol = scope.symbols.get(name);
            if (symbol != null) {
                return symbol;
            }
        }
        return null;
    }

    public Collection<Symbol> symbols() {
        return Collections.unmodifiableCollection(symbols.values());
    }
}
