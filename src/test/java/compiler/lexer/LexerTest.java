package compiler.lexer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class LexerTest {
    @ParameterizedTest
    @ValueSource(strings = {"", " \t", "// comment", " \t// comment"})
    void triviaOnlyInputReturnsStableEof(String source) {
        Lexer lexer = new Lexer(source);
        Token expected = new Token(TokenType.EOF, "", 1, source.length() + 1, null);
        assertEquals(expected, lexer.nextToken());
        assertEquals(expected, lexer.nextToken());
    }

    @Test
    void commentsPreserveNewlinesAndResumeAtNextToken() {
        Lexer lexer = new Lexer(" // first\r\n\t// second\n print // final");
        assertEquals(new Token(TokenType.NEWLINE, "\r\n", 1, 10, null), lexer.nextToken());
        assertEquals(new Token(TokenType.NEWLINE, "\n", 2, 11, null), lexer.nextToken());
        assertEquals(new Token(TokenType.PRINT, "print", 3, 2, null), lexer.nextToken());
        assertEquals(new Token(TokenType.EOF, "", 3, 16, null), lexer.nextToken());
    }

    @Test
    void callerCanRecoverAfterReportedErrorAndKeepSeparator() {
        Lexer lexer = new Lexer("  @bad;print 7");
        LexerException error = assertThrows(LexerException.class, lexer::nextToken);
        assertEquals(1, error.line());
        assertEquals(3, error.column());
        lexer.recover();
        assertEquals(TokenType.SEPARATOR, lexer.nextToken().type());
        assertEquals(TokenType.PRINT, lexer.nextToken().type());
        assertEquals(7L, lexer.nextToken().value());
        assertEquals(TokenType.EOF, lexer.nextToken().type());
    }
}
