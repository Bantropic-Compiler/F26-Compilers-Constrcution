package compiler.semantic.symbol;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class ScopeTest {

    private static VariableSymbol variable(String name) {
        return new VariableSymbol(name, null, VariableSymbol.Kind.LOCAL);
    }

    @Test
    void rootScopeIsGlobalAndChildIsNot() {
        Scope global = new Scope(null);
        Scope child = new Scope(global);

        assertTrue(global.isGlobal());
        assertFalse(child.isGlobal());
        assertSame(global, child.parent());
        assertNull(global.parent());
    }

    @Test
    void lookupWalksOutwardButLookupLocalDoesNot() {
        Scope global = new Scope(null);
        Scope child = new Scope(global);
        VariableSymbol outer = variable("a");
        global.declare(outer);

        assertSame(outer, child.lookup("a"));
        assertNull(child.lookupLocal("a"));
        assertSame(outer, global.lookupLocal("a"));
        assertNull(child.lookup("missing"));
    }

    @Test
    void innerDeclarationShadowsOuterOne() {
        Scope global = new Scope(null);
        Scope child = new Scope(global);
        VariableSymbol outer = variable("a");
        VariableSymbol inner = variable("a");
        global.declare(outer);
        child.declare(inner);

        assertSame(inner, child.lookup("a"));
        assertSame(outer, global.lookup("a"));
    }

    @Test
    void parentDoesNotSeeChildDeclarations() {
        Scope global = new Scope(null);
        Scope child = new Scope(global);
        child.declare(variable("local"));

        assertNull(global.lookup("local"));
    }

    @Test
    void symbolsAreListedInDeclarationOrder() {
        Scope scope = new Scope(null);
        scope.declare(variable("b"));
        scope.declare(variable("a"));
        scope.declare(new TypeSymbol("T", null));

        assertEquals(List.of("b", "a", "T"), scope.symbols().stream().map(Symbol::name).toList());
        assertThrows(UnsupportedOperationException.class, () -> scope.symbols().clear());
    }

    @Test
    void symbolsExposeNameDeclarationAndKind() {
        VariableSymbol symbol = new VariableSymbol("p", null, VariableSymbol.Kind.PARAMETER);

        assertEquals("p", symbol.name());
        assertNull(symbol.declaration());
        assertEquals(VariableSymbol.Kind.PARAMETER, symbol.kind());
        assertNull(symbol.type());
    }
}
