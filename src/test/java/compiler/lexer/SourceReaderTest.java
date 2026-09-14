package compiler.lexer;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SourceReaderTest {
    @Test
    void lookaheadDoesNotConsumeAndHandlesBounds() {
        SourceReader reader = new SourceReader("ab");
        assertEquals('a', reader.peek());
        assertEquals('b', reader.peek(1));
        assertEquals('\0', reader.peek(2));
        assertEquals('\0', reader.peek(Integer.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> reader.peek(-1));
        assertEquals(1, reader.column());
        assertEquals('a', reader.advance());
        assertEquals('b', reader.advance());
        for (int i = 0; i < 3; i++) assertEquals('\0', reader.advance());
        assertEquals(3, reader.column());
        assertTrue(reader.isAtEnd());
    }

    @Test
    void emptyInputHasStablePosition() {
        SourceReader reader = new SourceReader("");
        reader.advance();
        assertEquals(1, reader.line());
        assertEquals(1, reader.column());
        assertEquals(new Token(TokenType.EOF, "", 1, 1, null),
                CategoryTestDriver.scan("").tokens().get(0));
    }
}
