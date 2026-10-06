package compiler.ast;

/**
 * Base class for every AST node. Holds the position of the token the
 * node started at.
 */
public abstract class Node {

    public final int line;
    public final int column;

    protected Node(int line, int column) {
        this.line = line;
        this.column = column;
    }
}
