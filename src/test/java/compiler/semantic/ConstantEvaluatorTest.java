package compiler.semantic;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ConstantEvaluatorTest {

    private static Long value(String expression) {
        return ConstantEvaluator.integerValue(SemanticTestSupport.expression(expression));
    }

    @Test
    void evaluatesLiteralsAndSigns() {
        assertEquals(Long.valueOf(5), value("5"));
        assertEquals(Long.valueOf(-5), value("-5"));
        assertEquals(Long.valueOf(7), value("+7"));
        assertEquals(Long.valueOf(0), value("0"));
    }

    @Test
    void respectsOperatorPrecedenceAndParentheses() {
        assertEquals(Long.valueOf(14), value("2 + 3 * 4"));
        assertEquals(Long.valueOf(20), value("(2 + 3) * 4"));
        assertEquals(Long.valueOf(-1), value("2 - 3"));
        assertEquals(Long.valueOf(14), value("2 * (3 + 4)"));
    }

    @Test
    void divisionAndRemainderTruncateTowardZero() {
        assertEquals(Long.valueOf(3), value("7 / 2"));
        assertEquals(Long.valueOf(-3), value("-7 / 2"));
        assertEquals(Long.valueOf(1), value("7 % 3"));
        assertEquals(Long.valueOf(-1), value("-7 % 3"));
    }

    @Test
    void divisionAndRemainderByZeroAreNotConstants() {
        assertNull(value("1 / 0"));
        assertNull(value("1 % 0"));
        assertNull(value("5 / (2 - 2)"));
    }

    @Test
    void nonIntegerOrNonLiteralExpressionsAreNotConstants() {
        assertNull(value("x + 1"));
        assertNull(value("1.5"));
        assertNull(value("true"));
        assertNull(value("'a'"));
        assertNull(value("\"s\""));
        assertNull(value("f(1)"));
        assertNull(value("not 1"));
        assertNull(value("1 < 2"));
        assertNull(value("1 + 2.0"));
    }

    @Test
    void resultsOutsideThe32BitRangeAreNotConstants() {
        assertEquals(Long.valueOf(2147483647), value("2147483647"));
        assertNull(value("2147483648"));
        assertNull(value("2147483647 + 1"));
        assertEquals(Long.valueOf(-2147483648L), value("-2147483648"));
        assertNull(value("-2147483649"));
        assertNull(value("2147483647 * 2147483647 * 2147483647 * 2147483647"));
    }

    @Test
    void nullExpressionIsNotAConstant() {
        assertNull(ConstantEvaluator.integerValue(null));
    }
}
