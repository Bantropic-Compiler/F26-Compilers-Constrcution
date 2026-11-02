package compiler.semantic.optimize;

import compiler.ast.Program;
import compiler.semantic.SemanticModel;

@FunctionalInterface
public interface Optimization {

    /** Mutates the AST in place; returns true only when the tree changed. */
    boolean run(Program program, SemanticModel model);
}
