package compiler.ast.type;

import compiler.lexer.Token;
import compiler.parser.Parser;
import compiler.parser.ParserException;

public class PrimitiveTypeNode extends TypeNode {

    public enum Kind { INTEGER, REAL, BOOLEAN, CHAR }

    public final Kind kind;

    private PrimitiveTypeNode(int line, int column, Kind kind) {
        super(line, column);
        this.kind = kind;
    }

    public static PrimitiveTypeNode parse(Parser p) {
        Token token = p.getCurrent();
        Kind kind = switch (token.type()) {
            case INTEGER -> Kind.INTEGER;
            case REAL -> Kind.REAL;
            case BOOLEAN -> Kind.BOOLEAN;
            case CHAR -> Kind.CHAR;
            default -> throw new ParserException(
                    "expected a primitive type but found " + token.type(),
                    token.line(), token.column());
        };
        p.advance();
        return new PrimitiveTypeNode(token.line(), token.column(), kind);
    }
}
