package compiler.ast.declaration;

import compiler.ast.Node;
import compiler.ast.type.TypeNode;
import compiler.lexer.Token;
import compiler.lexer.TokenType;
import compiler.parser.Parser;

public class Parameter extends Node {

    public final String name;
    public final TypeNode type;

    private Parameter(int line, int column, String name, TypeNode type) {
        super(line, column);
        this.name = name;
        this.type = type;
    }

    /** ParameterDeclaration : Identifier : Type */
    public static Parameter parse(Parser p) {
        Token nameToken = p.expect(TokenType.IDENTIFIER);
        p.expect(TokenType.COLON);
        TypeNode type = TypeNode.parse(p);
        return new Parameter(nameToken.line(), nameToken.column(), (String) nameToken.value(), type);
    }
}
