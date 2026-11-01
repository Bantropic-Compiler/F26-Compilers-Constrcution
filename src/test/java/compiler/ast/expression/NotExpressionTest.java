package compiler.ast.expression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import compiler.ast.expression.expression.BinaryExpression;
import compiler.ast.expression.expression.UnaryExpression;
import compiler.ast.expression.literal.IntegerLiteral;
import compiler.lexer.Lexer;
import compiler.lexer.TokenType;
import compiler.parser.Parser;
import compiler.parser.ParserException;
import org.junit.jupiter.api.Test;

class NotExpressionTest {

    private static Expression parse(String source) {
        Parser parser = new Parser(new Lexer(source));
        Expression expression = Expression.parse(parser);
        parser.expect(TokenType.EOF);
        return expression;
    }

    @Test
    void notBeforeAnIntegerLiteralIsAUnaryExpression() {
        UnaryExpression not = assertInstanceOf(UnaryExpression.class, parse("not 1"));

        assertEquals(TokenType.NOT, not.op);
        assertEquals(1, assertInstanceOf(IntegerLiteral.class, not.operand).value);
    }

    @Test
    void notBindsTighterThanBinaryOperators() {
        BinaryExpression and = assertInstanceOf(BinaryExpression.class, parse("not 1 and 0"));

        assertEquals(TokenType.AND, and.op);
        assertEquals(TokenType.NOT, assertInstanceOf(UnaryExpression.class, and.left).op);
    }

    @Test
    void notBeforeANonNumericOperandIsStillASyntaxError() {
        assertThrows(ParserException.class, () -> parse("not true"));
        assertThrows(ParserException.class, () -> parse("not flag"));
    }
}
