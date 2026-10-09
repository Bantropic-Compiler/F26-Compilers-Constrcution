package compiler.ast.statement;

import compiler.ast.BlockNode;
import compiler.ast.expression.Expression;
import compiler.lexer.Token;
import compiler.lexer.TokenType;
import compiler.parser.Parser;

/** if Expression then Body [ else Body ] end */
public class IfStatement extends Statement {

    public final Expression condition;
    public final BlockNode thenBody;
    public final BlockNode elseBody; // null when there is no else branch

    private IfStatement(int line, int column, Expression condition,
                        BlockNode thenBody, BlockNode elseBody) {
        super(line, column);
        this.condition = condition;
        this.thenBody = thenBody;
        this.elseBody = elseBody;
    }

    public static IfStatement parse(Parser p) {
        Token start = p.expect(TokenType.IF);
        Expression condition = Expression.parse(p);
        p.expect(TokenType.THEN);
        BlockNode thenBody = BlockNode.parse(p);

        BlockNode elseBody = null;
        if (p.check(TokenType.ELSE)) {
            p.advance();
            elseBody = BlockNode.parse(p);
        }
        p.expect(TokenType.END);

        return new IfStatement(start.line(), start.column(), condition, thenBody, elseBody);
    }
}
