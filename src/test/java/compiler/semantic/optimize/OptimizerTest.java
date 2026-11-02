package compiler.semantic.optimize;

import compiler.ast.Program;
import compiler.ast.declaration.VariableDeclaration;
import compiler.ast.expression.literal.IntegerLiteral;
import compiler.semantic.SemanticAnalyzer;
import compiler.semantic.SemanticModel;
import compiler.semantic.SemanticTestSupport;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OptimizerTest {

    @Test
    void emptyPipelineReusesExistingAnalysis() {
        Program program = SemanticTestSupport.parse("var x is 1");
        var analysis = new SemanticAnalyzer().analyze(program);
        OptimizationResult result = new Optimizer().optimize(program, analysis);
        assertSame(analysis, result.semanticResult());
        assertSame(program, result.program());
        assertFalse(result.changed());
        assertFalse(result.hasErrors());
        assertNotNull(result.semanticResult().model().variableOf(program.declarations.get(0)));
    }

    @Test
    void invalidInputNeverRunsPasses() {
        Program program = SemanticTestSupport.parse("var x is unknown");
        var expected = new SemanticAnalyzer().analyze(program).diagnostics();
        OptimizationResult result = new Optimizer(List.of((tree, model) -> {
            fail("Pass must not run on invalid input");
            return false;
        })).optimize(program, new SemanticAnalyzer().analyze(program));
        assertTrue(result.hasErrors());
        assertFalse(result.changed());
        assertNull(result.failedPass());
        assertEquals(expected, result.semanticResult().diagnostics());
    }

    @Test
    void refreshesModelBetweenPassesAndRepeatsUntilStable() {
        Program program = SemanticTestSupport.parse("var x is 0");
        int[] calls = {0};
        Optimization first = (tree, model) -> {
            calls[0]++;
            VariableDeclaration variable = (VariableDeclaration) tree.declarations.get(0);
            assertTrue(model.hasType(variable.initializer));
            return replace(tree, 1, 2);
        };
        Optimization second = (tree, model) -> replace(tree, 0, 1);
        Optimizer optimizer = new Optimizer(List.of(first, second));
        OptimizationResult result = optimizer.optimize(program, new SemanticAnalyzer().analyze(program));
        assertFalse(result.hasErrors());
        assertTrue(result.changed());
        assertEquals(3, calls[0]);
        VariableDeclaration variable = (VariableDeclaration) program.declarations.get(0);
        assertEquals(2, ((IntegerLiteral) variable.initializer).value);
        assertTrue(result.semanticResult().model().hasType(variable.initializer));
        assertFalse(optimizer.optimize(program, result.semanticResult()).changed());
    }

    @Test
    void stopsImmediatelyAndIdentifiesFailingPass() {
        Program program = SemanticTestSupport.parse("var x : integer is 1");
        Optimization pass = new InvalidReplacement();
        OptimizationResult result = new Optimizer(List.of(pass, (tree, model) -> {
            fail("Must stop after semantic errors");
            return false;
        })).optimize(program, new SemanticAnalyzer().analyze(program));
        assertTrue(result.hasErrors());
        assertTrue(result.changed());
        assertEquals(InvalidReplacement.class.getName(), result.failedPass());
        assertEquals(new SemanticAnalyzer().analyze(program).diagnostics(),
                result.semanticResult().diagnostics());
    }

    private static boolean replace(Program tree, long from, long to) {
        VariableDeclaration variable = (VariableDeclaration) tree.declarations.get(0);
        IntegerLiteral literal = (IntegerLiteral) variable.initializer;
        if (literal.value != from) return false;
        variable.initializer = new IntegerLiteral(literal.line, literal.column, to);
        return true;
    }

    private static final class InvalidReplacement implements Optimization {
        public boolean run(Program program, SemanticModel model) {
            ((VariableDeclaration) program.declarations.get(0)).initializer =
                    SemanticTestSupport.expression("10 / 0");
            return true;
        }
    }
}
