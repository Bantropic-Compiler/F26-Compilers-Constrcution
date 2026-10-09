package compiler.ast.expression.accessor;

import compiler.ast.expression.Accessor;
import compiler.ast.expression.Expression;
import compiler.lexer.Token;
import compiler.lexer.TokenType;
import compiler.parser.Parser;

public class IndexAccess extends Accessor {

    public final Expression index;

    private IndexAccess(int line, int column, Expression index) {
        super(line, column);
        this.index = index;
    }

    public static IndexAccess parse(Parser p) {
        Token bracket = p.expect(TokenType.LBRACKET);
        Expression index = Expression.parse(p);
        p.expect(TokenType.RBRACKET);
        return new IndexAccess(bracket.line(), bracket.column(), index);
    }
}
