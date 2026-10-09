package compiler.ast.expression.literal;

import compiler.ast.expression.Expression;
import compiler.lexer.Token;
import compiler.lexer.TokenType;
import compiler.parser.Parser;

public class RealLiteral extends Expression {

    public final double value;

    private RealLiteral(int line, int column, double value) {
        super(line, column);
        this.value = value;
    }

    public static RealLiteral parse(Parser p) {
        Token token = p.expect(TokenType.REAL_LITERAL);
        return new RealLiteral(token.line(), token.column(), (Double) token.value());
    }
}
