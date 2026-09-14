package compiler.lexer;

/**
 * Reported for invalid characters, unterminated string/char literals,
 * malformed numeric literals, etc.
 */
public class LexerException extends RuntimeException {

    private final int line;
    private final int column;

    public LexerException(String message, int line, int column) {
        super(line + ":" + column + ": " + message);
        this.line = line;
        this.column = column;
    }

    public int line() { return line; }

    public int column() { return column; }
}
