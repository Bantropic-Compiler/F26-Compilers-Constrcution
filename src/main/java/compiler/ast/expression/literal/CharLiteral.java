package compiler.ast.expression.literal;

import compiler.ast.expression.Expression;
import compiler.lexer.Token;
import compiler.lexer.TokenType;
import compiler.parser.Parser;

public class CharLiteral extends Expression {

    public final int codePoint;

    private CharLiteral(int line, int column, int codePoint) {
        super(line, column);
        this.codePoint = codePoint;
    }

    public static CharLiteral parse(Parser p) {
        Token token = p.expect(TokenType.CHAR_LITERAL);
        return new CharLiteral(token.line(), token.column(), (Integer) token.value());
    }
}
