package compiler.ast.statement.routine;

import compiler.ast.expression.Expression;
import compiler.ast.expression.expression.RoutineCallExpression;
import compiler.ast.statement.Statement;
import compiler.lexer.Token;
import compiler.lexer.TokenType;
import compiler.parser.Parser;
import compiler.parser.ParserException;
import java.util.List;

/** A routine invocation used as a statement; its result, if any, is discarded. */
public class RoutineCallStatement extends Statement {

    public final String name;
    public final List<Expression> args;

    private RoutineCallStatement(int line, int column, String name, List<Expression> args) {
        super(line, column);
        this.name = name;
        this.args = List.copyOf(args);
    }

    public static RoutineCallStatement parse(Parser p) {
        return parse(p, p.expect(TokenType.IDENTIFIER));
    }

    public static RoutineCallStatement parse(Parser p, Token name) {
        if (!isCallAfterName(p.getCurrent().type())) {
            throw new ParserException("expected a call argument list or the end of a statement",
                    p.getCurrent().line(), p.getCurrent().column());
        }
        List<Expression> args = p.check(TokenType.LPAREN)
                ? RoutineCallExpression.parse(p, name).args
                : List.of();
        return new RoutineCallStatement(name.line(), name.column(), (String) name.value(), args);
    }

    public static boolean isCallAfterName(TokenType next) {
        return switch (next) {
            case LPAREN, NEWLINE, SEPARATOR, END, ELSE, EOF -> true;
            default -> false;
        };
    }
}
