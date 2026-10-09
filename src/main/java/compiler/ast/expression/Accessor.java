package compiler.ast.expression;

import compiler.ast.Node;

/** One step in a modifiable primary's field/index access chain. */
public abstract class Accessor extends Node {

    protected Accessor(int line, int column) {
        super(line, column);
    }
}
