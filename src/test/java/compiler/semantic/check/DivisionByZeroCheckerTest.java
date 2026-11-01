package compiler.semantic.check;

import static compiler.semantic.SemanticTestSupport.diagnostics;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DivisionByZeroCheckerTest {
    @ParameterizedTest
    @ValueSource(strings = {"0", "0.0", "-0.0", "(2 - 2)", "(1 / 2)",
            "(1.5 - 1.5)", "(0.0 + 1 / 2)", "(4 % 2)"})
    void rejectsConstantZeroDivisors(String divisor) {
        var errors = diagnostics("var x is 10 / " + divisor + "\n");
        assertEquals(1, errors.size());
        assertEquals("division by constant zero", errors.get(0).split(": ", 2)[1]);
    }

    @Test
    void reportsRemainderAtDivisor() {
        assertEquals(List.of("1:15: remainder by constant zero"), diagnostics("var x is 10 % 0\n"));
    }

    @Test
    void reportsDivisionAtDivisor() {
        assertEquals(List.of("1:15: division by constant zero"), diagnostics("var x is 10 / 0\n"));
    }

    @Test
    void preservesRealDivisionAndDoesNotGuessVariableValuesOrOverflow() {
        assertEquals(List.of(), diagnostics("""
                var n is 0
                var a is 10 / n
                var b is 10 / (1.0 / 2)
                var c is 10 / ((2147483647 + 1) * 0)
                var d is 10 % 3
                """));
    }

    @Test
    void nestedDivisionReportsOnlyTheActualZeroDivisor() {
        assertEquals(List.of("1:20: division by constant zero"), diagnostics("var x is 10 / (1 / 0)\n"));
    }

    @Test
    void typeErrorsDoNotProduceAdditionalZeroDiagnostics() {
        assertEquals(List.of("1:10: operator '/' cannot be applied to 'string' and 'integer'"),
                diagnostics("var x is \"text\" / 0\n"));
    }
}
