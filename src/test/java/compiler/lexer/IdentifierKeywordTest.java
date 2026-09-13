package compiler.lexer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IdentifierKeywordTest {

    @Test
    void recognizesEveryKeywordButNotItsLongerName() {
        String words = "var type is routine integer real boolean char string record array while loop "
                + "for in reverse if then else end print and or xor not true false return break continue";
        for (String word : words.split(" ")) {
            assertEquals(new Token(TokenType.valueOf(word.toUpperCase(java.util.Locale.ROOT)),
                    word, 1, 1, null), new Lexer(word).scanIdentifierOrKeyword());
            assertEquals(TokenType.IDENTIFIER,
                    new Lexer(word + "_1").scanIdentifierOrKeyword().type());
        }
        assertEquals(TokenType.IDENTIFIER, new Lexer("While").scanIdentifierOrKeyword().type());
        assertEquals("_value2", new Lexer("_value2").scanIdentifierOrKeyword().value());
    }

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