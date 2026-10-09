package compiler.ast.statement;

import compiler.ast.Node;
import compiler.parser.Parser;

/**
 * Stage 4 — owner: Arsen.
 * Stub so BlockNode compiles against Statement before the real
 * dispatcher (Assignment/RoutineCall/While/For/If/Print/...) is written.
 * ReturnStatement is implemented already (see ReturnStatement.java) —
 * Stage 5 needed it for desugaring "=> Expression".
 */
public abstract class Statement extends Node {

    protected Statement(int line, int column) {
        super(line, column);
    }

    public static Statement parse(Parser p) {
        throw new UnsupportedOperationException("Statement.parse() — Stage 4 (Arsen)");
    }
}
