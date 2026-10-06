package compiler.ast;

import compiler.lexer.Token;
import compiler.lexer.TokenType;
import compiler.parser.Parser;

public class TypeReferenceNode extends TypeNode {

    public final String name;

    private TypeReferenceNode(int line, int column, String name) {
        super(line, column);
        this.name = name;
    }

    public static TypeReferenceNode parse(Parser p) {
        Token token = p.expect(TokenType.IDENTIFIER);
        return new TypeReferenceNode(token.line(), token.column(), (String) token.value());
    }
}
