package compiler.ast.declaration;

import compiler.ast.Node;
import compiler.parser.Parser;
import compiler.parser.ParserException;

public abstract class Declaration extends Node {

    protected Declaration(int line, int column) {
        super(line, column);
    }

    /** SimpleDeclaration : VariableDeclaration | TypeDeclaration */
    public static Declaration parseSimple(Parser p) {
        return switch (p.getCurrent().type()) {
            case VAR -> VariableDeclaration.parse(p);
            case TYPE -> TypeDeclaration.parse(p);
            default -> throw new ParserException(
                    "expected a declaration but found " + p.getCurrent().type(),
                    p.getCurrent().line(), p.getCurrent().column());
        };
    }
}
