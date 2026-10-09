package compiler.ast.expression.expression;

import compiler.ast.expression.Expression;
import compiler.ast.expression.literal.IntegerLiteral;
import compiler.ast.expression.literal.RealLiteral;
import compiler.lexer.Token;
import compiler.lexer.TokenType;
import compiler.parser.Parser;
import compiler.parser.ParserException;

public class UnaryExpression extends Expression {

    public final TokenType op;
    public final Expression operand;

    private UnaryExpression(int line, int column, TokenType op, Expression operand) {
        super(line, column);
        this.op = op;
        this.operand = operand;
    }

    /** Signs precede integer or real literals. */
    public static UnaryExpression parse(Parser p) {
        if (!p.check(TokenType.PLUS) && !p.check(TokenType.MINUS)) {
            throw new ParserException("expected + or - before a numeric literal",
                    p.getCurrent().line(), p.getCurrent().column());
        }
        Token operator = p.advance();

        // The source grammar allows "not" only before an integer literal, while its
        // prose describes Boolean negation. Keep this branch disabled until clarified.
        // if (operator.type() == TokenType.NOT) {
        //     return new UnaryExpression(operator.line(), operator.column(), operator.type(),
        //             IntegerLiteral.parse(p));
        // }
        
        Expression operand = switch (p.getCurrent().type()) {
            case INTEGER_LITERAL -> IntegerLiteral.parse(p);
            case REAL_LITERAL -> RealLiteral.parse(p);
            default -> throw new ParserException(
                    "expected a numeric literal after " + operator.type(),
                    p.getCurrent().line(), p.getCurrent().column());
        };
        return new UnaryExpression(operator.line(), operator.column(), operator.type(),
                operand);
    }
}
