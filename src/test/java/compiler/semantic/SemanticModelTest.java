package compiler.semantic;

import static org.junit.jupiter.api.Assertions.*;

import compiler.ast.Program;
import compiler.ast.declaration.Declaration;
import compiler.semantic.symbol.RoutineSymbol;
import compiler.semantic.symbol.Scope;
import compiler.semantic.symbol.TypeSymbol;
import compiler.semantic.symbol.VariableSymbol;
import compiler.semantic.type.ErrorType;
import compiler.semantic.type.PrimitiveType;
import org.junit.jupiter.api.Test;

class SemanticModelTest {

    private final Program program = SemanticTestSupport.parse(
            "var a is 1\nvar b is 1\nroutine f()\ntype T is integer\n");
    private final Declaration first = program.declarations.get(0);
    private final Declaration second = program.declarations.get(1);

    @Test
    void typeOfUnknownNodeIsErrorType() {
        SemanticModel model = new SemanticModel();

        assertSame(ErrorType.INSTANCE, model.typeOf(first));
        assertFalse(model.hasType(first));
    }

    @Test
    void typesAreStoredPerNodeIdentity() {
        SemanticModel model = new SemanticModel();
        model.setType(first, PrimitiveType.INTEGER);

        assertSame(PrimitiveType.INTEGER, model.typeOf(first));
        assertTrue(model.hasType(first));
        assertFalse(model.hasType(second));
    }

    @Test
    void symbolsAreBoundPerNodeAndTypedAccessorsFilterByKind() {
        SemanticModel model = new SemanticModel();
        VariableSymbol variable = new VariableSymbol("a", first, VariableSymbol.Kind.GLOBAL);
        RoutineSymbol routine = new RoutineSymbol("f", (compiler.ast.declaration.RoutineDeclaration)
                program.declarations.get(2));
        TypeSymbol type = new TypeSymbol("T", program.declarations.get(3));
        model.bind(first, variable);
        model.bind(second, routine);
        model.bind(program.declarations.get(3), type);

        assertSame(variable, model.symbolOf(first));
        assertSame(variable, model.variableOf(first));
        assertNull(model.routineOf(first));
        assertNull(model.typeSymbolOf(first));
        assertSame(routine, model.routineOf(second));
        assertNull(model.variableOf(second));
        assertSame(type, model.typeSymbolOf(program.declarations.get(3)));
        assertNull(model.symbolOf(program.declarations.get(2)));
    }

    @Test
    void scopesAreStoredPerNodeAndGlobalScopeIsAlwaysAvailable() {
        SemanticModel model = new SemanticModel();
        Scope scope = new Scope(model.globalScope());
        model.setScope(first, scope);

        assertSame(scope, model.scopeOf(first));
        assertNull(model.scopeOf(second));
        assertTrue(model.globalScope().isGlobal());
    }
}
