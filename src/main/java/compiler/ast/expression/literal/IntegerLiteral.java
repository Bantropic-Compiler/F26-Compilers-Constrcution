package compiler.ast.expression.literal;

import compiler.ast.expression.Expression;
import compiler.lexer.Token;
import compiler.lexer.TokenType;
import compiler.parser.Parser;

public class IntegerLiteral extends Expression {

    public final long value;

    private IntegerLiteral(int line, int column, long value) {
        super(line, column);
        this.value = value;
    }

    public static IntegerLiteral parse(Parser p) {
        return fromToken(p.expect(TokenType.INTEGER_LITERAL));
    }

    static IntegerLiteral fromToken(Token token) {
        return new IntegerLiteral(token.line(), token.column(), (Long) token.value());
    }
}
