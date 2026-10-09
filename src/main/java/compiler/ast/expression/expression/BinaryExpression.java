package compiler.ast.expression.expression;

import compiler.ast.expression.Expression;
import compiler.lexer.Token;
import compiler.lexer.TokenType;

public class BinaryExpression extends Expression {

    public final TokenType op;
    public final Expression left;
    public final Expression right;

    private BinaryExpression(int line, int column, TokenType op, Expression left, Expression right) {
        super(line, column);
        this.op = op;
        this.left = left;
        this.right = right;
    }

    public static BinaryExpression wrap(Token operator, Expression left, Expression right) {
        return new BinaryExpression(left.line, left.column, operator.type(), left, right);
    }
}
