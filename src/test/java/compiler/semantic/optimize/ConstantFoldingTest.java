package compiler.semantic.optimize;

import compiler.ast.declaration.VariableDeclaration;
import compiler.semantic.*;
import compiler.semantic.visitor.SourcePrinter;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConstantFoldingTest {
    private void folds(String expression, String expected) {
        var program = SemanticTestSupport.parse("var x is " + expression);
        var original = ((VariableDeclaration) program.declarations.get(0)).initializer;
        var result = new Optimizer().optimize(program, new SemanticAnalyzer().analyze(program));
        assertFalse(result.hasErrors(), () -> result.semanticResult().diagnostics().toString());
        assertTrue(result.changed());
        assertEquals(SourcePrinter.print(SemanticTestSupport.parse("var x is " + expected)), SourcePrinter.print(program));
        var literal = ((VariableDeclaration) program.declarations.get(0)).initializer;
        assertEquals(original.line, literal.line);
        assertEquals(original.column, literal.column);
        assertFalse(new Optimizer().optimize(program, result.semanticResult()).changed());
        assertFalse(SemanticTestSupport.analyze(SourcePrinter.print(program)).hasErrors());
    }

    @Test void arithmetic() {
        folds("5 + 3 * 2", "11");
        folds("7 / 2 + 0.5", "3.5");
        folds("-7 / 2", "-3");
        folds("7 % 3", "1");
        folds("-2147483648 + 0", "-2147483648");
    }
    @Test void comparisonsAndLogic() {
        folds("3 < 5", "true");
        folds("1 and (not 0)", "true");
        folds("true xor true", "false");
        folds("1 = 1.0", "true");
        folds("'a' < 'b'", "true");
        folds("\"ab\" = \"ab\"", "true");
        folds("\"a\" + 'b'", "\"ab\"");
        folds("'a' + \"b\"", "\"ab\"");
    }
    @Test void unsafeExpressionsRemain() {
        for (String expression : new String[]{"2147483647 + 1", "(2147483647 + 1) - 1", "10 / 0", "1 % 0", "(-2147483648) / (-1)"}) {
            var program = SemanticTestSupport.parse("var x is " + expression);
            var analysis = new SemanticAnalyzer().analyze(program);
            var node = ((VariableDeclaration) program.declarations.get(0)).initializer;
            assertNull(ConstantEvaluator.valueOf(node, analysis.model()), expression);
            assertNull(ConstantEvaluator.integerValue(node), expression);
        }
    }
}
