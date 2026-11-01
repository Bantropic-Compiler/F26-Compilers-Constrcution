package compiler.semantic;

import static org.junit.jupiter.api.Assertions.*;

import compiler.lexer.TokenType;
import org.junit.jupiter.api.Test;

class OperatorsTest {

    @Test
    void symbolsMatchSourceSpelling() {
        assertEquals("+", Operators.symbol(TokenType.PLUS));
        assertEquals("-", Operators.symbol(TokenType.MINUS));
        assertEquals("*", Operators.symbol(TokenType.STAR));
        assertEquals("/", Operators.symbol(TokenType.SLASH));
        assertEquals("%", Operators.symbol(TokenType.PERCENT));
        assertEquals("<", Operators.symbol(TokenType.LT));
        assertEquals("<=", Operators.symbol(TokenType.LE));
        assertEquals(">", Operators.symbol(TokenType.GT));
        assertEquals(">=", Operators.symbol(TokenType.GE));
        assertEquals("=", Operators.symbol(TokenType.EQ));
        assertEquals("/=", Operators.symbol(TokenType.NEQ));
        assertEquals("and", Operators.symbol(TokenType.AND));
        assertEquals("or", Operators.symbol(TokenType.OR));
        assertEquals("xor", Operators.symbol(TokenType.XOR));
        assertEquals("not", Operators.symbol(TokenType.NOT));
    }

    @Test
    void precedenceGrowsFromLogicalToMultiplicative() {
        assertEquals(1, Operators.precedence(TokenType.AND));
        assertEquals(1, Operators.precedence(TokenType.OR));
        assertEquals(1, Operators.precedence(TokenType.XOR));
        for (TokenType relation : new TokenType[] {TokenType.LT, TokenType.LE, TokenType.GT,
                TokenType.GE, TokenType.EQ, TokenType.NEQ}) {
            assertEquals(2, Operators.precedence(relation));
        }
        assertEquals(3, Operators.precedence(TokenType.PLUS));
        assertEquals(3, Operators.precedence(TokenType.MINUS));
        assertEquals(4, Operators.precedence(TokenType.STAR));
        assertEquals(4, Operators.precedence(TokenType.SLASH));
        assertEquals(4, Operators.precedence(TokenType.PERCENT));
    }

    @Test
    void precedenceOfNonBinaryOperatorIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Operators.precedence(TokenType.NOT));
        assertThrows(IllegalArgumentException.class, () -> Operators.precedence(TokenType.IDENTIFIER));
    }
}
