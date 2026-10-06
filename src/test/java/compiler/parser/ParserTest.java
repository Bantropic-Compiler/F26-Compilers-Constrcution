package compiler.parser;

import compiler.lexer.Lexer;
import compiler.lexer.TokenType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ParserTest {

    @Test
    void currentHoldsFirstTokenOnConstruction() {
        Parser p = new Parser(new Lexer("var x"));
        assertEquals(TokenType.VAR, p.getCurrent().type());
    }

    @Test
    void checkDoesNotConsume() {
        Parser p = new Parser(new Lexer("var x"));
        assertTrue(p.check(TokenType.VAR));
        assertTrue(p.check(TokenType.VAR)); // still there, check() doesn't move
    }

    @Test
    void advanceConsumesAndMovesToNextToken() {
        Parser p = new Parser(new Lexer("var x"));
        var consumed = p.advance();

        assertEquals(TokenType.VAR, consumed.type());
        assertEquals(TokenType.IDENTIFIER, p.getCurrent().type());
    }

    @Test
    void expectConsumesOnMatch() {
        Parser p = new Parser(new Lexer("var x"));
        var token = p.expect(TokenType.VAR);

        assertEquals(TokenType.VAR, token.type());
        assertEquals(TokenType.IDENTIFIER, p.getCurrent().type());
    }

    @Test
    void expectThrowsOnMismatchWithoutConsuming() {
        Parser p = new Parser(new Lexer("var x"));
        ParserException ex = assertThrows(ParserException.class, () -> p.expect(TokenType.TYPE));

        assertEquals(1, ex.line());
        assertEquals(1, ex.column());
        assertEquals(TokenType.VAR, p.getCurrent().type()); // not consumed
    }

    @Test
    void skipSeparatorsSkipsNewlinesAndSemicolons() {
        Parser p = new Parser(new Lexer("\n;\n var x"));
        p.skipSeparators();

        assertEquals(TokenType.VAR, p.getCurrent().type());
    }

    @Test
    void skipSeparatorsStopsAtNonSeparatorToken() {
        Parser p = new Parser(new Lexer("var x"));
        p.skipSeparators(); // nothing to skip

        assertEquals(TokenType.VAR, p.getCurrent().type());
    }
}
