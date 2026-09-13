package compiler.lexer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WhitespaceCommentsTest {

    @Test
    void skipsSpacesAndTabs() {
        Lexer lexer = new Lexer("  \t\tabc");
        lexer.skipWhitespace();

        assertEquals('a', lexer.reader().peek());
    }

    @Test
    void newlineProducesSignificantToken() {
        // \n must NOT be dropped like other whitespace
        Lexer lexer = new Lexer("\nrest");
        Token token = lexer.scanNewline();

        assertEquals(TokenType.NEWLINE, token.type());
        assertEquals(1, token.line());
        assertEquals(1, token.column());
        assertEquals('r', lexer.reader().peek());
    }

    @Test
    void separatorProducesToken() {
        Lexer lexer = new Lexer(";rest");
        Token token = lexer.scanSeparator();

        assertEquals(TokenType.SEPARATOR, token.type());
        assertEquals(';', token.lexeme().charAt(0));
    }

    @Test
    void lineCommentIsSkippedUpToNewline() {
        Lexer lexer = new Lexer("// comment text\nrest");
        Token token = lexer.scanSlash();

        assertNull(token); // a comment produces no token
        assertEquals('\n', lexer.reader().peek()); // '\n' left for the next nextToken() call
    }

    @Test
    void lineCommentAtEndOfFileWithNoTrailingNewline() {
        Lexer lexer = new Lexer("// just a comment");
        Token token = lexer.scanSlash();

        assertNull(token);
        assertTrue(lexer.reader().isAtEnd());
    }

    @Test
    void singleSlashIsDivisionOperator() {
        Lexer lexer = new Lexer("/ 2");
        Token token = lexer.scanSlash();

        assertEquals(TokenType.SLASH, token.type());
        assertEquals("/", token.lexeme());
    }

    @Test
    void slashEqualsIsNotEqualOperator() {
        Lexer lexer = new Lexer("/= 2");
        Token token = lexer.scanSlash();

        assertEquals(TokenType.NEQ, token.type());
        assertEquals("/=", token.lexeme());
    }
}