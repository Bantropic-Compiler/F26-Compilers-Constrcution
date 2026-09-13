package compiler.lexer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IdentifierKeywordTest {

    @Test
    void recognizesIdentifier() {
        Lexer lexer = new Lexer("foo");
        Token token = lexer.scanIdentifierOrKeyword();

        assertEquals(TokenType.IDENTIFIER, token.type());
        assertEquals("foo", token.lexeme());
        assertEquals("foo", token.value());
    }

    @Test
    void recognizesKeyword() {
        Lexer lexer = new Lexer("while");
        Token token = lexer.scanIdentifierOrKeyword();

        assertEquals(TokenType.WHILE, token.type());
        assertEquals("while", token.lexeme());
        assertNull(token.value());
    }

    @Test
    void keywordAndIdentifierAreNotConfused() {
        // "endpoint" must NOT lex as END + "point" — maximal munch
        Lexer lexer = new Lexer("endpoint");
        Token token = lexer.scanIdentifierOrKeyword();

        assertEquals(TokenType.IDENTIFIER, token.type());
        assertEquals("endpoint", token.lexeme());
    }

    @Test
    void stopsAtFirstNonIdentifierCharacter() {
        Lexer lexer = new Lexer("abc123 rest");
        Token token = lexer.scanIdentifierOrKeyword();

        assertEquals("abc123", token.lexeme());
        assertEquals(' ', lexer.reader().peek());
    }

    @Test
    void recordsStartPositionNotEndPosition() {
        Lexer lexer = new Lexer("abcdef");
        Token token = lexer.scanIdentifierOrKeyword();

        assertEquals(1, token.line());
        assertEquals(1, token.column());
    }
}