package compiler.ast.expression;

import compiler.ast.Node;
import compiler.parser.Parser;

/**
 * Stub so the rest of the AST compiles against Expression before the
 * real hierarchy (BinaryExpression, literals, ModifiablePrimaryNode,
 * RoutineCallExpression, ...) is written.
 */
public abstract class Expression extends Node {

    protected Expression(int line, int column) {
        super(line, column);
    }

    public static Expression parse(Parser p) {
        throw new UnsupportedOperationException("Expression.parse() — Stage 3 (Arsen)");
    }
}
