package compiler.semantic.check;

import static compiler.TestPrograms.read;
import static compiler.semantic.SemanticTestSupport.diagnostics;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DivisionByZeroCheckerTest {
    @ParameterizedTest
    @ValueSource(strings = {
            "zero-integer-zero.i",
            "zero-real-zero.i",
            "zero-negative-real-zero.i",
            "zero-integer-subtraction.i",
            "zero-integer-division.i",
            "zero-real-subtraction.i",
            "zero-mixed-arithmetic.i",
            "zero-remainder.i"})
    void rejectsConstantZeroDivisors(String testCase) {
        var errors = diagnostics(read("semantic/division-by-zero/" + testCase));
        assertEquals(1, errors.size());
        assertEquals("division by constant zero", errors.get(0).split(": ", 2)[1]);
    }

    @Test
    void reportsRemainderAtDivisor() {
        assertEquals(List.of("1:15: remainder by constant zero"), diagnostics(read("semantic/division-by-zero/reports-remainder-at-divisor.i")));
    }

    @Test
    void reportsDivisionAtDivisor() {
        assertEquals(List.of("1:15: division by constant zero"), diagnostics(read("semantic/division-by-zero/reports-division-at-divisor.i")));
    }

    @Test
    void preservesRealDivisionAndDoesNotGuessVariableValuesOrOverflow() {
        assertEquals(List.of(), diagnostics(read("semantic/division-by-zero/preserves-real-division-and-does-not-guess-variable-values-or-overflow.i")));
    }

    @Test
    void nestedDivisionReportsOnlyTheActualZeroDivisor() {
        assertEquals(List.of("1:20: division by constant zero"), diagnostics(read("semantic/division-by-zero/nested-division-reports-only-the-actual-zero-divisor.i")));
    }

    @Test
    void typeErrorsDoNotProduceAdditionalZeroDiagnostics() {
        assertEquals(List.of("1:10: operator '/' cannot be applied to 'string' and 'integer'"),
                diagnostics(read("semantic/division-by-zero/type-errors-do-not-produce-additional-zero-diagnostics.i")));
    }
}
