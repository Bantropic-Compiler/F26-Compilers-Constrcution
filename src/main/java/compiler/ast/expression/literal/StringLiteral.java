package compiler.ast.expression.literal;

import compiler.ast.expression.Expression;
import compiler.lexer.Token;
import compiler.lexer.TokenType;
import compiler.parser.Parser;

public class StringLiteral extends Expression {

    public final String value;

    private StringLiteral(int line, int column, String value) {
        super(line, column);
        this.value = value;
    }

    public static StringLiteral parse(Parser p) {
        Token token = p.expect(TokenType.STRING_LITERAL);
        return new StringLiteral(token.line(), token.column(), (String) token.value());
    }
}
