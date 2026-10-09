package compiler.ast.expression.expression;

import java.util.ArrayList;
import java.util.List;

import compiler.ast.expression.Expression;
import compiler.lexer.Token;
import compiler.lexer.TokenType;
import compiler.parser.Parser;

public class RoutineCallExpression extends Expression {

    public final String name;
    public final List<Expression> args;

    private RoutineCallExpression(int line, int column, String name, List<Expression> args) {
        super(line, column);
        this.name = name;
        this.args = List.copyOf(args);
    }

    public static RoutineCallExpression parse(Parser p) {
        return parse(p, p.expect(TokenType.IDENTIFIER));
    }

    public static RoutineCallExpression parse(Parser p, Token name) {
        p.expect(TokenType.LPAREN);
        List<Expression> args = new ArrayList<>();
        if (!p.check(TokenType.RPAREN)) {
            args.add(Expression.parse(p));
            while (p.check(TokenType.COMMA)) {
                p.advance();
                args.add(Expression.parse(p));
            }
        }
        p.expect(TokenType.RPAREN);
        return new RoutineCallExpression(name.line(), name.column(), (String) name.value(), args);
    }
}
