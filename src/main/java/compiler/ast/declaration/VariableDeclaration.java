package compiler.ast.declaration;

import compiler.ast.expression.Expression;
import compiler.ast.type.TypeNode;
import compiler.lexer.Token;
import compiler.lexer.TokenType;
import compiler.parser.Parser;
import compiler.parser.ParserException;

public class VariableDeclaration extends Declaration {

    public final String name;
    public final TypeNode type;          // nullable
    public final Expression initializer; // nullable

    private VariableDeclaration(int line, int column, String name, TypeNode type, Expression initializer) {
        super(line, column);
        this.name = name;
        this.type = type;
        this.initializer = initializer;
    }

    /**
     * VariableDeclaration
     *  : var Identifier : Type [ is Expression ]
     *  | var Identifier is Expression
     */
    public static VariableDeclaration parse(Parser p) {
        Token start = p.expect(TokenType.VAR);
        Token nameToken = p.expect(TokenType.IDENTIFIER);
        String name = (String) nameToken.value();

        TypeNode type = null;
        Expression initializer = null;

        if (p.check(TokenType.COLON)) {
            p.advance();
            type = TypeNode.parse(p);
            if (p.check(TokenType.IS)) {
                p.advance();
                initializer = Expression.parse(p);
            }
        } else if (p.check(TokenType.IS)) {
            p.advance();
            initializer = Expression.parse(p);
        } else {
            throw new ParserException(
                    "variable declaration needs a type, an initializer, or both",
                    p.getCurrent().line(), p.getCurrent().column());
        }

        return new VariableDeclaration(start.line(), start.column(), name, type, initializer);
    }
}
