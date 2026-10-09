package compiler.ast.expression.literal;

import compiler.ast.expression.Expression;
import compiler.lexer.Token;
import compiler.parser.Parser;
import compiler.parser.ParserException;

public class BooleanLiteral extends Expression {

    public final boolean value;

    private BooleanLiteral(int line, int column, boolean value) {
        super(line, column);
        this.value = value;
    }

    public static BooleanLiteral parse(Parser p) {
        Token token = p.getCurrent();
        boolean value = switch (token.type()) {
            case TRUE -> true;
            case FALSE -> false;
            default -> throw new ParserException(
                    "expected a boolean literal but found " + token.type(),
                    token.line(), token.column());
        };
        p.advance();
        return new BooleanLiteral(token.line(), token.column(), value);
    }
}
