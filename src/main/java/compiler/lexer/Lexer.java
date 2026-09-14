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

    /**
     * Returns the next token, skipping spaces, tabs and comments.
     * Newlines and semicolons remain significant tokens. At end of input,
     * repeated calls return EOF at the same position.
     *
     * @throws LexerException on malformed input; call recover() before continuing
     */
    public Token nextToken() {
        while (true) {
            skipWhitespace();
            if (reader.isAtEnd()) {
                return new Token(TokenType.EOF, "", reader.line(), reader.column(), null);
            }
            char c = reader.peek();
            if (isIdentifierStart(c)) return scanIdentifierOrKeyword();
            if (isDigit(c)) return scanNumber();
            Token token = switch (c) {
                case '"' -> scanString();
                case '\'' -> scanChar();
                case '\r', '\n' -> scanNewline();
                case ';' -> scanSeparator();
                default -> scanOperatorOrDelimiter();
            };
            if (token != null) return token;
        }
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
        TokenType type = TokenType.keywordType(word);
        Object value = (type == TokenType.IDENTIFIER) ? word : null;
        return new Token(type, word, startLine, startColumn, value);
    }

    private boolean isIdentifierStart(char c) {
        return Character.isLetter(c) || c == '_';
    }

    private boolean isIdentifierPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    Token scanNumber() {
        int startLine = reader.line();
        int startColumn = reader.column();
        StringBuilder buffer = new StringBuilder();
        while (isDigit(reader.peek())) buffer.append(reader.advance());
        boolean real = reader.peek() == '.' && reader.peek(1) != '.';
        if (real) {
            buffer.append(reader.advance());
            if (!isDigit(reader.peek())) {
                throw new LexerException("expected digit after decimal point", startLine, startColumn);
            }
            while (isDigit(reader.peek())) buffer.append(reader.advance());
        }
        if (isIdentifierStart(reader.peek())) {
            throw new LexerException("invalid numeric literal", startLine, startColumn);
        }
        String lexeme = buffer.toString();
        try {
            if (real) {
                double value = Double.parseDouble(lexeme);
                if (!Double.isFinite(value)) {
                    throw new NumberFormatException();
                }
                return new Token(TokenType.REAL_LITERAL, lexeme, startLine, startColumn, value);
            }
            return new Token(TokenType.INTEGER_LITERAL, lexeme, startLine, startColumn,
                    Long.parseLong(lexeme));
        } catch (NumberFormatException e) {
            throw new LexerException("numeric literal exceeds token value capacity", startLine, startColumn);
        }
    }

    Token scanString() {
        return scanQuoted('"', TokenType.STRING_LITERAL);
    }

    Token scanChar() {
        return scanQuoted('\'', TokenType.CHAR_LITERAL);
    }

    private Token scanQuoted(char quote, TokenType type) {
        int line = reader.line();
        int column = reader.column();
        StringBuilder buffer = new StringBuilder();
        StringBuilder value = new StringBuilder();
        buffer.append(reader.advance());
        while (!reader.isAtEnd() && reader.peek() != quote) {
            if (reader.peek() == '\n' || reader.peek() == '\r') {
                throw new LexerException("newline in quoted literal", line, column);
            }
            char c = reader.advance();
            buffer.append(c);
            if (c == '\\') {
                if (reader.isAtEnd()) {
                    throw new LexerException("unterminated escape sequence", line, column);
                }
                if (reader.peek() == '\n' || reader.peek() == '\r') {
                    throw new LexerException("newline in quoted literal", line, column);
                }
                char escaped = reader.advance();
                buffer.append(escaped);
                c = switch (escaped) {
                    case 'n' -> '\n';
                    case 't' -> '\t';
                    case '\\' -> '\\';
                    case '"' -> '"';
                    case '\'' -> '\'';
                    default -> throw new LexerException("unsupported escape sequence: \\" + escaped,
                            line, column);
                };
            }
            value.append(c);
        }
        if (reader.isAtEnd()) {
            throw new LexerException("unterminated quoted literal", line, column);
        }
        buffer.append(reader.advance());
        String decoded = value.toString();
        // Reject isolated UTF-16 surrogates; char means a Unicode scalar value.
        for (int i = 0; i < decoded.length(); i++) {
            char c = decoded.charAt(i);
            if (Character.isHighSurrogate(c) && i + 1 < decoded.length()
                    && Character.isLowSurrogate(decoded.charAt(i + 1))) {
                i++;
            } else if (Character.isSurrogate(c)) {
                throw new LexerException("invalid Unicode in quoted literal", line, column);
            }
        }
        if (type == TokenType.CHAR_LITERAL) {
            if (decoded.codePointCount(0, decoded.length()) != 1) {
                throw new LexerException("char literal must contain exactly one Unicode code point",
                        line, column);
            }
            return new Token(type, buffer.toString(), line, column, decoded.codePointAt(0));
        }
        return new Token(type, buffer.toString(), line, column, decoded);
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    Token scanOperatorOrDelimiter() {
        if (reader.peek() == '/') return scanSlash();
        int line = reader.line();
        int column = reader.column();
        char first = reader.peek();
        TokenType type = switch (first) {
            case ':' -> TokenType.COLON;
            case '.' -> TokenType.DOT;
            case ',' -> TokenType.COMMA;
            case '(' -> TokenType.LPAREN;
            case ')' -> TokenType.RPAREN;
            case '[' -> TokenType.LBRACKET;
            case ']' -> TokenType.RBRACKET;
            case '+' -> TokenType.PLUS;
            case '-' -> TokenType.MINUS;
            case '*' -> TokenType.STAR;
            case '%' -> TokenType.PERCENT;
            case '<' -> TokenType.LT;
            case '>' -> TokenType.GT;
            case '=' -> TokenType.EQ;
            default -> throw new LexerException("unexpected character '" + first + "'", line, column);
        };
        reader.advance();
        String lexeme = String.valueOf(first);
        if (reader.peek() == '=' && (first == ':' || first == '<' || first == '>')) {
            lexeme += reader.advance();
            type = switch (first) {
                case ':' -> TokenType.ASSIGN;
                case '<' -> TokenType.LE;
                default -> TokenType.GE;
            };
        } else if (first == '.' && reader.peek() == '.') {
            lexeme += reader.advance();
            type = TokenType.RANGE;
        }
        return new Token(type, lexeme, line, column, null);
    }

    /**
     * Panic-mode recovery after a category scanner throws LexerException.
     * Leaves whitespace and significant separators for the caller to handle.
     * The caller collects the exception, recovers, and resumes scanning.
     */
    public void recover() {
        while (!reader.isAtEnd()) {
            char c = reader.peek();
            if (c == ' ' || c == '\t' || c == '\r' || c == '\n' || c == ';') return;
            reader.advance();
        }
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
        String lexeme = String.valueOf(reader.advance());
        if (lexeme.equals("\r") && reader.peek() == '\n') lexeme += reader.advance();
        return new Token(TokenType.NEWLINE, lexeme, startLine, startColumn, null);
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
        while (reader.peek() != '\n' && reader.peek() != '\r' && !reader.isAtEnd()) {
            reader.advance();
        }
    }
}
