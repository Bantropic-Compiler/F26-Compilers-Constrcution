package compiler.parser;

import compiler.lexer.Lexer;
import compiler.lexer.Token;
import compiler.lexer.TokenType;

/**
 * Token cursor shared by every AST node's static parse() method.
 * Owns the single lookahead token ("current") and the primitives
 * every production is built from: check/expect/advance/skipSeparators.
 */
public class Parser {

    private final Lexer lexer;
    private Token current;

    public Parser(Lexer lexer) {
        this.lexer = lexer;
        this.current = lexer.nextToken();
    }

    /** The lookahead token, not yet consumed. */
    public Token getCurrent() {
        return current;
    }

    public boolean check(TokenType type) {
        return current.type() == type;
    }

    /** Returns and consumes the current token, advancing to the next one. */
    public Token advance() {
        Token token = current;
        current = lexer.nextToken();
        return token;
    }

    /**
     * Consumes the current token if it matches, otherwise throws
     * ParserException. Returns the consumed token.
     */
    public Token expect(TokenType type) {
        if (!check(type)) {
            throw new ParserException(
                    "expected " + type + " but found " + current.type(),
                    current.line(), current.column());
        }
        return advance();
    }

    /** Consumes zero or more NEWLINE/SEPARATOR tokens. */
    public void skipSeparators() {
        while (check(TokenType.NEWLINE) || check(TokenType.SEPARATOR)) {
            advance();
        }
    }
}
