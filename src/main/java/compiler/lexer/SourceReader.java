package compiler.lexer;

public class SourceReader {

    private final String source;
    private int pos = 0;
    private int line = 1;
    private int column = 1;

    public SourceReader(String source) {
        this.source = source;
    }

    /** Returns current character without consuming it, or '\0' at end of input. */
    public char peek() {
        if (pos >= source.length()) {
            return '\0';
        }
        return source.charAt(pos);
    }

    /** Returns character at `offset` position ahead from current without consuming it, or '\0' past end of input. */
    public char peek(int offset) {
        int target = pos + offset;
        if (target >= source.length()) {
            return '\0';
        }
        return source.charAt(target);
    }

    /** Consumes and returns the current character, updating line or/and column. */
    public char advance() {
        char current = peek();
        pos++;
        if (current == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        return current;
    }

    public boolean isAtEnd() {
        return pos >= source.length();
    }

    public int line() {
        return line;
    }

    public int column() {
        return column;
    }
}
