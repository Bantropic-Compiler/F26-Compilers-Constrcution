package compiler.lexer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class LiteralTest {
    @Test
    void numbersPreserveTextValueAndBoundary() {
        Lexer lexer = new Lexer("  0012 3.140..9");
        lexer.skipWhitespace();
        assertEquals(new Token(TokenType.INTEGER_LITERAL, "0012", 1, 3, 12L), lexer.scanNumber());
        lexer.skipWhitespace();
        assertEquals(new Token(TokenType.REAL_LITERAL, "3.140", 1, 8, 3.14), lexer.scanNumber());
        assertEquals(TokenType.RANGE, lexer.scanOperatorOrDelimiter().type());
        assertEquals(9L, lexer.scanNumber().value());
    }

    @Test
    void rangeDoesNotBecomeDecimalAndMinusIsSeparate() {
        var result = CategoryTestDriver.scan("-2147483648 1..n p.x");
        assertTrue(result.errors().isEmpty());
        assertEquals(java.util.List.of(TokenType.MINUS, TokenType.INTEGER_LITERAL,
                TokenType.INTEGER_LITERAL, TokenType.RANGE, TokenType.IDENTIFIER,
                TokenType.IDENTIFIER, TokenType.DOT, TokenType.IDENTIFIER, TokenType.EOF),
                result.tokens().stream().map(Token::type).toList());
        assertEquals(2147483648L, result.tokens().get(1).value());
    }

    @ParameterizedTest
    @ValueSource(strings = {"3.", "3.x", "12abc", "1e3", "2_0", "9223372036854775808"})
    void malformedNumbersReportTokenStart(String source) {
        Lexer lexer = new Lexer("\n  " + source);
        lexer.scanNewline();
        lexer.skipWhitespace();
        LexerException error = assertThrows(LexerException.class, lexer::scanNumber);
        assertEquals(2, error.line());
        assertEquals(3, error.column());
        assertTrue(error.getMessage().startsWith("2:3: "));
    }

    @Test
    void numericStorageLimits() {
        assertEquals(Long.MAX_VALUE, new Lexer("9223372036854775807").scanNumber().value());
        assertThrows(LexerException.class, () -> new Lexer("9".repeat(310) + ".0").scanNumber());
    }

    @Test
    void stringDecodesAllSupportedEscapesAndPreservesLexeme() {
        String source = "\"a\\n\\t\\\\\\\"\\' // b\"";
        Token token = new Lexer(source).scanString();
        assertEquals(new Token(TokenType.STRING_LITERAL, source, 1, 1, "a\n\t\\\"' // b"), token);
        assertEquals("", new Lexer("\"\"").scanString().value());
        assertEquals("😀", new Lexer("\"😀\"").scanString().value());
    }

    @Test
    void charsAreUnicodeCodePoints() {
        assertEquals((int) 'a', new Lexer("'a'").scanChar().value());
        assertEquals(0x1F600, new Lexer("'😀'").scanChar().value());
        String[] escapes = {"n", "t", "\\", "\"", "'"};
        int[] values = {'\n', '\t', '\\', '"', '\''};
        for (int i = 0; i < escapes.length; i++) {
            String source = "'\\" + escapes[i] + "'";
            assertEquals(new Token(TokenType.CHAR_LITERAL, source, 1, 1, values[i]),
                    new Lexer(source).scanChar());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"abc", "\"abc\\", "\"a\nb\"", "\"a\rb\"", "\"\\q\"",
            "\"a\\\nb\"", "\"\uD800\""})
    void invalidStrings(String source) {
        assertThrows(LexerException.class, () -> new Lexer(source).scanString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"''", "'ab'", "'😀a'", "'a", "'\\q'", "'\n'", "'\uDC00'"})
    void invalidChars(String source) {
        assertThrows(LexerException.class, () -> new Lexer(source).scanChar());
    }
}
