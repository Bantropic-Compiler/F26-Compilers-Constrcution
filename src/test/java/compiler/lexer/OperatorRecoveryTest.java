package compiler.lexer;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OperatorRecoveryTest {
    @Test
    void allOperatorsUseLongestMatch() {
        String[] lexemes = {":=", ":", ".", "..", ",", "(", ")", "[", "]", "+", "-", "*",
                "/", "%", "<", "<=", ">", ">=", "=", "/="};
        TokenType[] types = {TokenType.ASSIGN, TokenType.COLON, TokenType.DOT, TokenType.RANGE,
                TokenType.COMMA, TokenType.LPAREN, TokenType.RPAREN, TokenType.LBRACKET,
                TokenType.RBRACKET, TokenType.PLUS, TokenType.MINUS, TokenType.STAR,
                TokenType.SLASH, TokenType.PERCENT, TokenType.LT, TokenType.LE, TokenType.GT,
                TokenType.GE, TokenType.EQ, TokenType.NEQ};
        for (int i = 0; i < lexemes.length; i++) {
            Lexer lexer = new Lexer(lexemes[i] + "x");
            assertEquals(new Token(types[i], lexemes[i], 1, 1, null), lexer.scanOperatorOrDelimiter());
            assertEquals('x', lexer.reader().peek());
        }
        var result = CategoryTestDriver.scan(":=<=...>==");
        assertTrue(result.errors().isEmpty());
        assertEquals(List.of(TokenType.ASSIGN, TokenType.LE, TokenType.RANGE, TokenType.DOT,
                TokenType.GE, TokenType.EQ, TokenType.EOF),
                result.tokens().stream().map(Token::type).toList());
    }

    @Test
    void recoveryCollectsMultipleErrorsAndPreservesSeparators() {
        var result = CategoryTestDriver.scan("@bad; 12abc\n\"unclosed\nprint 7");
        assertEquals(3, result.errors().size());
        assertEquals("1:1: unexpected character '@'", result.errors().get(0).getMessage());
        assertEquals(7, result.errors().get(1).column());
        assertEquals(2, result.errors().get(2).line());
        assertEquals(List.of(TokenType.SEPARATOR, TokenType.NEWLINE, TokenType.NEWLINE,
                TokenType.PRINT, TokenType.INTEGER_LITERAL, TokenType.EOF),
                result.tokens().stream().map(Token::type).toList());
    }

    @Test
    void recoveryAlwaysMakesProgressOnInvalidCharactersAndEof() {
        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            var result = CategoryTestDriver.scan("\u0000 \f @");
            assertEquals(3, result.errors().size());
            assertEquals(List.of(TokenType.EOF), result.tokens().stream().map(Token::type).toList());
            assertEquals(1, CategoryTestDriver.scan("\"trailing\\").errors().size());
        });
    }

    @Test
    void commentsAndCrLfRetainExactPositions() {
        var result = CategoryTestDriver.scan("// comment\r\n\tprint;\rnext\n");
        assertTrue(result.errors().isEmpty());
        assertEquals(new Token(TokenType.NEWLINE, "\r\n", 1, 11, null), result.tokens().get(0));
        assertEquals(new Token(TokenType.PRINT, "print", 2, 2, null), result.tokens().get(1));
        assertEquals(new Token(TokenType.IDENTIFIER, "next", 3, 1, "next"), result.tokens().get(4));
        assertEquals(new Token(TokenType.EOF, "", 4, 1, null), result.tokens().get(6));
    }
}
