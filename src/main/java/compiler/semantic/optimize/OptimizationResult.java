package compiler.semantic.optimize;

import compiler.ast.Program;
import compiler.semantic.SemanticResult;

/** The tree and its latest analysis, including on failure. No rollback is performed. */
public record OptimizationResult(
        Program program, SemanticResult semanticResult, boolean changed, String failedPass) {

    public boolean hasErrors() {
        return semanticResult.hasErrors();
    }
}
