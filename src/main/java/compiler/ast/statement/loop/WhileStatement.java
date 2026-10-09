package compiler.ast.statement.loop;

import compiler.ast.BlockNode;
import compiler.ast.expression.Expression;
import compiler.ast.statement.Statement;
import compiler.lexer.Token;
import compiler.lexer.TokenType;
import compiler.parser.Parser;

/** while Expression loop Body end */
public class WhileStatement extends Statement {

    public final Expression condition;
    public final BlockNode body;

    private WhileStatement(int line, int column, Expression condition, BlockNode body) {
        super(line, column);
        this.condition = condition;
        this.body = body;
    }

    public static WhileStatement parse(Parser p) {
        Token start = p.expect(TokenType.WHILE);
        Expression condition = Expression.parse(p);
        p.expect(TokenType.LOOP);
        BlockNode body = BlockNode.parse(p);
        p.expect(TokenType.END);
        return new WhileStatement(start.line(), start.column(), condition, body);
    }
}
