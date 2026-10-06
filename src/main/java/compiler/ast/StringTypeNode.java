package compiler.ast;

import compiler.lexer.Token;
import compiler.lexer.TokenType;
import compiler.parser.Parser;

public class StringTypeNode extends TypeNode {

    private StringTypeNode(int line, int column) {
        super(line, column);
    }

    public static StringTypeNode parse(Parser p) {
        Token token = p.expect(TokenType.STRING);
        return new StringTypeNode(token.line(), token.column());
    }
}
