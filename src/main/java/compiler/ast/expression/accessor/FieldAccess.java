package compiler.ast.expression.accessor;

import compiler.ast.expression.Accessor;
import compiler.lexer.Token;
import compiler.lexer.TokenType;
import compiler.parser.Parser;

public class FieldAccess extends Accessor {

    public final String name;

    private FieldAccess(int line, int column, String name) {
        super(line, column);
        this.name = name;
    }

    public static FieldAccess parse(Parser p) {
        Token dot = p.expect(TokenType.DOT);
        Token name = p.expect(TokenType.IDENTIFIER);
        return new FieldAccess(dot.line(), dot.column(), (String) name.value());
    }
}
