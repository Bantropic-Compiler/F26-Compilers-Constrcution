package compiler.semantic.check;

import static org.junit.jupiter.api.Assertions.*;

import compiler.ast.Program;
import compiler.ast.declaration.Parameter;
import compiler.ast.declaration.RoutineDeclaration;
import compiler.ast.declaration.TypeDeclaration;
import compiler.ast.declaration.VariableDeclaration;
import compiler.ast.expression.ModifiablePrimaryNode;
import compiler.ast.expression.expression.BinaryExpression;
import compiler.ast.expression.expression.RoutineCallExpression;
import compiler.ast.statement.PrintStatement;
import compiler.ast.statement.loop.ForStatement;
import compiler.ast.statement.routine.ReturnStatement;
import compiler.ast.type.TypeReferenceNode;
import compiler.semantic.Diagnostic;
import compiler.semantic.Diagnostics;
import compiler.semantic.SemanticModel;
import compiler.semantic.SemanticTestSupport;
import compiler.semantic.symbol.RoutineSymbol;
import compiler.semantic.symbol.TypeSymbol;
import compiler.semantic.symbol.VariableSymbol;
import java.util.List;
import org.junit.jupiter.api.Test;

class NameResolverTest {

    private record Resolved(Program program, SemanticModel model, List<String> diagnostics) {
    }

    private static Resolved resolve(String source) {
        Program program = SemanticTestSupport.parse(source);
        SemanticModel model = new SemanticModel();
        Diagnostics diagnostics = new Diagnostics();
        new NameResolver().run(program, model, diagnostics);
        return new Resolved(program, model,
                diagnostics.sorted().stream().map(Diagnostic::toString).toList());
    }

    private static List<String> diagnostics(String source) {
        return resolve(source).diagnostics();
    }

    @Test
    void undeclaredNamesAreReportedAtTheirPosition() {
        assertEquals(List.of("2:5: 'y' is not declared", "2:10: 'x' is not declared"), diagnostics("""
                routine t() is
                    y := x
                end
                """));
    }

    @Test
    void aNameIsVisibleOnlyAfterItsOwnDeclarationEnds() {
        assertEquals(List.of("1:10: 'g' is not declared"), diagnostics("var g is g\n"));
        assertEquals(List.of("2:14: 'x' is not declared"), diagnostics("""
                routine t() is
                    var x is x
                end
                """));
    }

    @Test
    void globalsDeclaredLaterAreNotVisibleToEarlierRoutines() {
        assertEquals(List.of("2:11: 'later' is not declared"), diagnostics("""
                routine t() is
                    print later
                end
                var later is 1
                """));
    }

    @Test
    void innerDeclarationsShadowOuterOnes() {
        assertEquals(List.of(), diagnostics("""
                var x is 1
                routine t(x: integer) is
                    var x is 2
                    if true then
                        var x is 3
                        print x
                    end
                end
                """));
    }

    @Test
    void redeclarationInTheSameScopeIsReportedAtTheSecondDeclaration() {
        assertEquals(List.of(
                "2:1: 'a' is already declared in this scope",
                "3:1: 'a' is already declared in this scope",
                "6:5: 'b' is already declared in this scope"), diagnostics("""
                var a is 1
                var a is 2
                type a is integer
                routine t() is
                    var b is 1
                    var b is 2
                end
                """));
    }

    @Test
    void variablesAndTypesShareOneNamespace() {
        assertEquals(List.of("2:1: 'T' is already declared in this scope"),
                diagnostics("type T is integer\nvar T: integer\n"));
    }

    @Test
    void duplicateParametersAreReportedAtTheSecondParameter() {
        assertEquals(List.of("1:23: parameter 'a' is already declared"),
                diagnostics("routine f(a: integer, a: integer) is\nend\n"));
    }

    @Test
    void aTypePositionRejectsAVariableName() {
        assertEquals(List.of("2:26: 'a' is not a type"),
                diagnostics("var a is 1\nroutine f(a: integer, b: a) is\nend\n"));
    }

    @Test
    void loopVariableLivesOnlyInsideTheLoop() {
        assertEquals(List.of("5:11: 'i' is not declared"), diagnostics("""
                routine t() is
                    for i in 1..3 loop
                        print i
                    end
                    print i
                end
                """));
    }

    @Test
    void rangeExpressionsAreResolvedOutsideTheLoopScope() {
        assertEquals(List.of("2:14: 'i' is not declared"), diagnostics("""
                routine t() is
                    for i in i..3 loop
                    end
                end
                """));
    }

    @Test
    void loopBodyIsANestedScopeSoItMayShadowTheLoopVariableButNotRedeclare() {
        assertEquals(List.of("4:9: 'j' is already declared in this scope"), diagnostics("""
                routine t() is
                    for j in 1..3 loop
                        var j is 2
                        var j is 3
                    end
                end
                """));
    }

    @Test
    void routineNameIsVisibleFromItsHeaderOnSoRecursionWorks() {
        assertEquals(List.of(), diagnostics("""
                routine fact(n: integer): integer is
                    return fact(n - 1)
                end
                """));
    }

    @Test
    void callingARoutineBeforeItsDeclarationIsAnError() {
        assertEquals(List.of("1:10: 'later' is not declared"), diagnostics("""
                var g is later()
                routine later(): integer => 1
                """));
    }

    @Test
    void forwardDeclarationAllowsEarlierCalls() {
        assertEquals(List.of(), diagnostics("""
                routine later(): integer
                routine t() is
                    print later()
                end
                routine later(): integer => 1
                """));
    }

    @Test
    void namesMustDenoteWhatTheirPositionRequires() {
        assertEquals(List.of(
                "7:5: 'T' is not a variable",
                "8:5: 'r' is not a variable",
                "9:5: 'v' is not a routine",
                "10:12: 'v' is not a type",
                "11:12: 'r' is not a type",
                "12:5: 'T' is not a routine"), diagnostics("""
                var v is 1
                type T is integer
                routine r() is
                end
                routine t() is
                    v := 1
                    T := 2
                    r := 3
                    v()
                    var w: v
                    var z: r
                    T()
                end
                """));
    }

    @Test
    void unknownTypeNamesAreReported() {
        assertEquals(List.of("1:8: 'Missing' is not declared"), diagnostics("var x: Missing\n"));
    }

    @Test
    void aTypeCannotReferToItselfInsideItsOwnDefinition() {
        assertEquals(List.of("2:15: 'A' is not declared"), diagnostics("""
                type A is record
                    var next: A
                end
                """));
    }

    @Test
    void forwardDeclarationsMustBeDefinedExactlyOnce() {
        assertEquals(List.of(
                "1:1: routine 'a' is declared but never defined",
                "2:1: routine 'b' is declared but never defined",
                "3:1: routine 'b' is already declared",
                "6:1: routine 'c' is already defined",
                "10:1: routine 'd' is already declared"), diagnostics("""
                routine a(): integer
                routine b()
                routine b()
                routine c() is
                end
                routine c() is
                end
                routine d() is
                end
                routine d()
                """));
    }

    @Test
    void forwardDeclarationFollowedByDefinitionIsFine() {
        Resolved resolved = resolve("routine f(): integer\nroutine f(): integer => 1\n");
        RoutineDeclaration forward = (RoutineDeclaration) resolved.program().declarations.get(0);
        RoutineDeclaration definition = (RoutineDeclaration) resolved.program().declarations.get(1);

        assertEquals(List.of(), resolved.diagnostics());
        RoutineSymbol symbol = resolved.model().routineOf(forward);
        assertSame(symbol, resolved.model().routineOf(definition));
        assertSame(forward, symbol.forwardDeclaration());
        assertSame(definition, symbol.definition());
        assertTrue(symbol.isDefined());
    }

    @Test
    void routineNamesClashWithVariablesAndTypes() {
        assertEquals(List.of("2:1: 'f' is already declared in this scope"),
                diagnostics("var f is 1\nroutine f() is\nend\n"));
        assertEquals(List.of("3:1: 'g' is already declared in this scope"),
                diagnostics("routine g() is\nend\nvar g is 1\n"));
    }

    @Test
    void recordFieldNamesMustBeUniqueButMayReuseOuterNames() {
        assertEquals(List.of("3:5: field 'a' is already declared in this record"), diagnostics("""
                type R is record
                    var a: integer
                    var a: string
                end
                """));
        assertEquals(List.of(), diagnostics("var a is 1\ntype R is record\n    var a: integer\nend\n"));
        assertEquals(List.of("2:12: 'Missing' is not declared"),
                diagnostics("type R is record\n    var a: Missing\nend\n"));
    }

    @Test
    void everyResolvedUseIsBoundToItsSymbol() {
        Resolved resolved = resolve("""
                var g is 1
                type T is integer
                routine f(p: T): integer is
                    var l is p + g
                    for i in 1..l loop
                        print i
                    end
                    return f(l)
                end
                """);
        SemanticModel model = resolved.model();
        List<?> declarations = resolved.program().declarations;
        VariableDeclaration global = (VariableDeclaration) declarations.get(0);
        TypeDeclaration type = (TypeDeclaration) declarations.get(1);
        RoutineDeclaration routine = (RoutineDeclaration) declarations.get(2);
        Parameter parameter = routine.parameters.get(0);
        VariableDeclaration local = (VariableDeclaration) routine.body.items.get(0);
        ForStatement loop = (ForStatement) routine.body.items.get(1);
        ReturnStatement result = (ReturnStatement) routine.body.items.get(2);
        BinaryExpression sum = (BinaryExpression) local.initializer;

        assertEquals(List.of(), resolved.diagnostics());
        assertEquals(VariableSymbol.Kind.GLOBAL, model.variableOf(global).kind());
        assertEquals(VariableSymbol.Kind.PARAMETER, model.variableOf(parameter).kind());
        assertEquals(VariableSymbol.Kind.LOCAL, model.variableOf(local).kind());
        assertEquals(VariableSymbol.Kind.LOOP, model.variableOf(loop).kind());
        TypeSymbol typeSymbol = model.typeSymbolOf(type);
        assertSame(typeSymbol, model.symbolOf((TypeReferenceNode) parameter.type));
        assertSame(model.variableOf(parameter), model.symbolOf((ModifiablePrimaryNode) sum.left));
        assertSame(model.variableOf(global), model.symbolOf((ModifiablePrimaryNode) sum.right));
        assertSame(model.variableOf(loop),
                model.symbolOf(((PrintStatement) loop.body.items.get(0)).args.get(0)));
        assertSame(model.variableOf(local), model.symbolOf(loop.rangeEnd));
        assertSame(model.routineOf(routine), model.symbolOf((RoutineCallExpression) result.value));
    }

    @Test
    void scopesFormTheDocumentedNesting() {
        Resolved resolved = resolve("""
                routine f(p: integer) is
                    for i in 1..p loop
                        print i
                    end
                end
                """);
        SemanticModel model = resolved.model();
        RoutineDeclaration routine = (RoutineDeclaration) resolved.program().declarations.get(0);
        ForStatement loop = (ForStatement) routine.body.items.get(0);

        assertSame(model.globalScope(), model.scopeOf(resolved.program()));
        assertSame(model.globalScope(), model.scopeOf(routine).parent());
        assertSame(model.scopeOf(routine), model.scopeOf(routine.body).parent());
        assertSame(model.scopeOf(routine.body), model.scopeOf(loop).parent());
        assertSame(model.scopeOf(loop), model.scopeOf(loop.body).parent());
        assertNotNull(model.scopeOf(routine).lookupLocal("p"));
        assertNotNull(model.scopeOf(loop).lookupLocal("i"));
        assertNull(model.scopeOf(routine.body).lookupLocal("i"));
    }

    @Test
    void unresolvedNamesStayUnbound() {
        Resolved resolved = resolve("routine t() is\n    y := x\n    nothing()\nend\n");
        RoutineDeclaration routine = (RoutineDeclaration) resolved.program().declarations.get(0);
        compiler.ast.statement.AssignmentStatement assignment =
                (compiler.ast.statement.AssignmentStatement) routine.body.items.get(0);

        assertNull(model(resolved).symbolOf(assignment.target));
        assertNull(model(resolved).symbolOf(assignment.value));
        assertNull(model(resolved).symbolOf(routine.body.items.get(1)));
    }

    private static SemanticModel model(Resolved resolved) {
        return resolved.model();
    }
}
