package compiler.lexer;

/**
 * All token categories the lexer can produce.
 */
public enum TokenType {

    // literals
    IDENTIFIER,
    INTEGER_LITERAL,
    REAL_LITERAL,
    CHAR_LITERAL,
    STRING_LITERAL,

    // keywords
    VAR, TYPE, IS, ROUTINE,
    INTEGER, REAL, BOOLEAN, CHAR, STRING,
    RECORD, ARRAY,
    WHILE, LOOP, FOR, IN, REVERSE,
    IF, THEN, ELSE, END,
    PRINT,
    AND, OR, XOR, NOT,
    TRUE, FALSE,
    RETURN, BREAK, CONTINUE,

    // operators / delimiters
    ASSIGN,        // :=
    COLON,         // :
    DOT,           // .
    RANGE,         // ..
    COMMA,         // ,
    LPAREN, RPAREN,
    LBRACKET, RBRACKET,
    PLUS, MINUS, STAR, SLASH, PERCENT,
    LT, LE, GT, GE, EQ, NEQ,

    // structural
    NEWLINE,
    SEPARATOR,     // ;
    EOF;

    /** Returns the keyword type, or IDENTIFIER if the word is not a keyword. */
    public static TokenType keywordType(String word) {
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
}
