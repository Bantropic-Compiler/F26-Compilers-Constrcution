package compiler.ast.statement.loop;

import compiler.ast.statement.Statement;
import compiler.lexer.Token;
import compiler.lexer.TokenType;
import compiler.parser.Parser;

public class BreakStatement extends Statement {

    private BreakStatement(int line, int column) {
        super(line, column);
    }

    public static BreakStatement parse(Parser p) {
        Token start = p.expect(TokenType.BREAK);
        return new BreakStatement(start.line(), start.column());
    }
}
