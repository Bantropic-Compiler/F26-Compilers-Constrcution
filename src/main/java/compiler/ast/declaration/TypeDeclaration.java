package compiler.ast.declaration;

import compiler.ast.type.TypeNode;
import compiler.lexer.Token;
import compiler.lexer.TokenType;
import compiler.parser.Parser;

public class TypeDeclaration extends Declaration {

    public final String name;
    public final TypeNode type;

    private TypeDeclaration(int line, int column, String name, TypeNode type) {
        super(line, column);
        this.name = name;
        this.type = type;
    }

    public static TypeDeclaration parse(Parser p) {
        Token start = p.expect(TokenType.TYPE);
        Token nameToken = p.expect(TokenType.IDENTIFIER);
        p.expect(TokenType.IS);
        TypeNode type = TypeNode.parse(p);
        return new TypeDeclaration(start.line(), start.column(), (String) nameToken.value(), type);
    }
}
