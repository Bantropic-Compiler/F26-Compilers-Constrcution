package compiler.ast.statement.loop;

import compiler.ast.BlockNode;
import compiler.ast.expression.Expression;
import compiler.ast.statement.Statement;
import compiler.lexer.Token;
import compiler.lexer.TokenType;
import compiler.parser.Parser;

/** for Identifier in Expression [ .. Expression ] [ reverse ] loop Body end */
public class ForStatement extends Statement {

    public final String loopVar;
    public final Expression rangeStart;
    public final Expression rangeEnd; // null for array iteration
    public final boolean reverse;
    public final BlockNode body;

    private ForStatement(int line, int column, String loopVar, Expression rangeStart,
                         Expression rangeEnd, boolean reverse, BlockNode body) {
        super(line, column);
        this.loopVar = loopVar;
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeEnd;
        this.reverse = reverse;
        this.body = body;
    }

    public static ForStatement parse(Parser p) {
        Token start = p.expect(TokenType.FOR);
        Token variable = p.expect(TokenType.IDENTIFIER);
        p.expect(TokenType.IN);
        Expression rangeStart = Expression.parse(p);

        Expression rangeEnd = null;
        if (p.check(TokenType.RANGE)) {
            p.advance();
            rangeEnd = Expression.parse(p);
        }

        boolean reverse = false;
        if (p.check(TokenType.REVERSE)) {
            p.advance();
            reverse = true;
        }

        p.expect(TokenType.LOOP);
        BlockNode body = BlockNode.parse(p);
        p.expect(TokenType.END);
        return new ForStatement(start.line(), start.column(),
                (String) variable.value(), rangeStart, rangeEnd, reverse, body);
    }
}
