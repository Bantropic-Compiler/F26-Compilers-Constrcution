package compiler.ast.statement;

import compiler.ast.expression.Expression;
import compiler.lexer.Token;
import compiler.lexer.TokenType;
import compiler.parser.Parser;
import java.util.ArrayList;
import java.util.List;

/** print Expression { , Expression } */
public class PrintStatement extends Statement {

    public final List<Expression> args;

    private PrintStatement(int line, int column, List<Expression> args) {
        super(line, column);
        this.args = List.copyOf(args);
    }

    public static PrintStatement parse(Parser p) {
        Token start = p.expect(TokenType.PRINT);
        List<Expression> args = new ArrayList<>();
        args.add(Expression.parse(p));
        while (p.check(TokenType.COMMA)) {
            p.advance();
            args.add(Expression.parse(p));
        }
        return new PrintStatement(start.line(), start.column(), args);
    }
}
