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
    EOF
}
