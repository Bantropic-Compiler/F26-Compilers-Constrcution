package compiler.ast.expression.expression;

import compiler.ast.expression.Expression;
import compiler.lexer.Token;
import compiler.lexer.TokenType;
import compiler.parser.Parser;
import compiler.parser.ParserException;

public class UnaryExpression extends Expression {

    public final TokenType op;
    public Expression operand;

    public UnaryExpression(int line, int column, TokenType op, Expression operand) {
        super(line, column);
        this.op = op;
        this.operand = operand;
    }

    /** A sign or not applies to the primary expression that follows it. */
    public static UnaryExpression parse(Parser p) {
        if (!p.check(TokenType.PLUS) && !p.check(TokenType.MINUS) && !p.check(TokenType.NOT)) {
            throw new ParserException("expected +, - or not",
                    p.getCurrent().line(), p.getCurrent().column());
        }
        Token operator = p.advance();
        Expression operand = Expression.parsePrimary(p);
        return new UnaryExpression(operator.line(), operator.column(), operator.type(), operand);
    }
}
