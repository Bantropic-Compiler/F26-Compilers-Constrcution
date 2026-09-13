package compiler.lexer;

/**
 * @param type   token category
 * @param lexeme the exact source text of the token
 * @param line   1-based line number
 * @param column 1-based column number (start of the token)
 * @param value  Long for integers, Double for reals, Integer Unicode code point
 *               for chars, String for strings and identifiers;
 *               null for everything else
 */
public record Token(TokenType type, String lexeme, int line, int column, Object value) {
}
