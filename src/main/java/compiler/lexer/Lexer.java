package compiler.lexer;

public class Lexer {

    private final SourceReader reader;

    public Lexer(String source) {
        this.reader = new SourceReader(source);
    }

    /** Package-private accessor for white-box tests. */
    SourceReader reader() {
        return reader;
    }

    /** Returns the next token, or an EOF token at end of input. */
    public Token nextToken() {
        // TODO
        throw new UnsupportedOperationException("not implemented");
    }

    // --- token category scanners, one method per category ---

    Token scanIdentifierOrKeyword() {
        int startLine = reader.line();
        int startColumn = reader.column();
        StringBuilder buffer = new StringBuilder();

        while (isIdentifierPart(reader.peek())) {
            buffer.append(reader.advance());
        }

        String word = buffer.toString();
        TokenType type = keywordType(word);
        Object value = (type == TokenType.IDENTIFIER) ? word : null;
        return new Token(type, word, startLine, startColumn, value);
    }

    private boolean isIdentifierStart(char c) {
        return Character.isLetter(c) || c == '_';
    }

    private boolean isIdentifierPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    private TokenType keywordType(String word) {
        return switch (word) {
            case "var" -> TokenType.VAR;
            case "type" -> TokenType.TYPE;
            case "is" -> TokenType.IS;
            case "routine" -> TokenType.ROUTINE;
            case "integer" -> TokenType.INTEGER;
            case "real" -> TokenType.REAL;
            case "boolean" -> TokenType.BOOLEAN;
            case "char" -> TokenType.CHAR;
            case "string" -> TokenType.STRING;
            case "record" -> TokenType.RECORD;
            case "array" -> TokenType.ARRAY;
            case "while" -> TokenType.WHILE;
            case "loop" -> TokenType.LOOP;
            case "for" -> TokenType.FOR;
            case "in" -> TokenType.IN;
            case "reverse" -> TokenType.REVERSE;
            case "if" -> TokenType.IF;
            case "then" -> TokenType.THEN;
            case "else" -> TokenType.ELSE;
            case "end" -> TokenType.END;
            case "print" -> TokenType.PRINT;
            case "and" -> TokenType.AND;
            case "or" -> TokenType.OR;
            case "xor" -> TokenType.XOR;
            case "not" -> TokenType.NOT;
            case "true" -> TokenType.TRUE;
            case "false" -> TokenType.FALSE;
            case "return" -> TokenType.RETURN;
            case "break" -> TokenType.BREAK;
            case "continue" -> TokenType.CONTINUE;
            default -> TokenType.IDENTIFIER;
        };
    }

    Token scanNumber() {
        throw new UnsupportedOperationException("not implemented");
    }

    Token scanString() {
        throw new UnsupportedOperationException("not implemented");
    }

    Token scanChar() {
        throw new UnsupportedOperationException("not implemented");
    }

    Token scanOperatorOrDelimiter() {
        throw new UnsupportedOperationException("not implemented");
    }

    // --- whitespace / newline / separator / comments ---

    /** Consumes spaces and tabs. Does Not touch '\n'. */
    void skipWhitespace() {
        while (reader.peek() == ' ' || reader.peek() == '\t') {
            reader.advance();
        }
    }

    Token scanNewline() {
        int startLine = reader.line();
        int startColumn = reader.column();
        reader.advance(); // consume '\n'
        return new Token(TokenType.NEWLINE, "\n", startLine, startColumn, null);
    }

    Token scanSeparator() {
        int startLine = reader.line();
        int startColumn = reader.column();
        reader.advance(); // consume ';'
        return new Token(TokenType.SEPARATOR, ";", startLine, startColumn, null);
    }

    /**
     * Handles every meaning of '/': line comment ("//"), the "/=" (not-equal)
     * operator, or plain division.
     *
     * Returns null when it consumed a comment — that produces no token, so
     * nextToken() must loop and scan again instead of returning null upward.
     */
    Token scanSlash() {
        int startLine = reader.line();
        int startColumn = reader.column();
        reader.advance(); // consume the first '/'

        if (reader.peek() == '/') {
            skipLineComment();
            return null;
        }
        if (reader.peek() == '=') {
            reader.advance();
            return new Token(TokenType.NEQ, "/=", startLine, startColumn, null);
        }
        return new Token(TokenType.SLASH, "/", startLine, startColumn, null);
    }

    private void skipLineComment() {
        while (reader.peek() != '\n' && !reader.isAtEnd()) {
            reader.advance();
        }
    }
}