package compiler.ast.statement.loop;

import compiler.ast.statement.Statement;
import compiler.lexer.Token;
import compiler.lexer.TokenType;
import compiler.parser.Parser;

public class ContinueStatement extends Statement {

    private ContinueStatement(int line, int column) {
        super(line, column);
    }

    public static ContinueStatement parse(Parser p) {
        Token start = p.expect(TokenType.CONTINUE);
        return new ContinueStatement(start.line(), start.column());
    }
}
