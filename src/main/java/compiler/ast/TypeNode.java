package compiler.ast;

import compiler.parser.Parser;
import compiler.parser.ParserException;

public abstract class TypeNode extends Node {

    protected TypeNode(int line, int column) {
        super(line, column);
    }

    public static TypeNode parse(Parser p) {
        return switch (p.getCurrent().type()) {
            case INTEGER, REAL, BOOLEAN, CHAR -> PrimitiveTypeNode.parse(p);
            case STRING -> StringTypeNode.parse(p);
            case ARRAY -> ArrayTypeNode.parse(p);
            case RECORD -> RecordTypeNode.parse(p);
            case IDENTIFIER -> TypeReferenceNode.parse(p);
            default -> throw new ParserException(
                    "expected a type but found " + p.getCurrent().type(),
                    p.getCurrent().line(), p.getCurrent().column());
        };
    }
}
