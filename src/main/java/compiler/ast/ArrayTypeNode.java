package compiler.ast;

import compiler.lexer.Token;
import compiler.lexer.TokenType;
import compiler.parser.Parser;

public class ArrayTypeNode extends TypeNode {

    public final Expression sizeExpr; // nullable — sizeless for routine parameters
    public final TypeNode elementType;

    private ArrayTypeNode(int line, int column, Expression sizeExpr, TypeNode elementType) {
        super(line, column);
        this.sizeExpr = sizeExpr;
        this.elementType = elementType;
    }

    public static ArrayTypeNode parse(Parser p) {
        Token start = p.expect(TokenType.ARRAY);
        p.expect(TokenType.LBRACKET);
        Expression sizeExpr = p.check(TokenType.RBRACKET) ? null : Expression.parse(p);
        p.expect(TokenType.RBRACKET);
        TypeNode elementType = TypeNode.parse(p);
        return new ArrayTypeNode(start.line(), start.column(), sizeExpr, elementType);
    }
}
