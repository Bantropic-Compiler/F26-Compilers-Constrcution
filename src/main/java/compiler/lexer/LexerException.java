package compiler.lexer;

/**
 * Reported for invalid characters, unterminated string/char literals,
 * malformed numeric literals, etc.
 */
public class LexerException extends RuntimeException {

    public LexerException(String message, int line, int column) {
        super(line + ":" + column + ": " + message);
    }
}
