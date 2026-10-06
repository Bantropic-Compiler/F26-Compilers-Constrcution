package compiler.parser;

/**
 * Reported for syntax errors: an unexpected token where the grammar
 * requires a specific one, or no production matches the current token.
 */
public class ParserException extends RuntimeException {

    private final int line;
    private final int column;

    public ParserException(String message, int line, int column) {
        super(line + ":" + column + ": " + message);
        this.line = line;
        this.column = column;
    }

    public int line() { return line; }

    public int column() { return column; }
}
