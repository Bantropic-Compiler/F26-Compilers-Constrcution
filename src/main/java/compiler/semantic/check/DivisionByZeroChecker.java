package compiler.semantic.check;

import compiler.ast.Program;
import compiler.ast.expression.expression.BinaryExpression;
import compiler.lexer.TokenType;
import compiler.semantic.ConstantEvaluator;
import compiler.semantic.Diagnostics;
import compiler.semantic.SemanticModel;
import compiler.semantic.visitor.AstVisitor;

/** C6: rejects division and remainder with a statically zero divisor. */
public final class DivisionByZeroChecker extends AstVisitor implements Check {
    private SemanticModel model;
    private Diagnostics diagnostics;

    @Override
    public void run(Program program, SemanticModel model, Diagnostics diagnostics) {
        this.model = model;
        this.diagnostics = diagnostics;
        visit(program);
    }

    @Override
    protected void visitBinary(BinaryExpression node) {
        super.visitBinary(node);
        if ((node.op == TokenType.SLASH || node.op == TokenType.PERCENT)
                && !model.typeOf(node).isError()) {
            Double divisor = ConstantEvaluator.numericValue(node.right, model);
            if (divisor != null && divisor == 0.0) {
                diagnostics.report(node.right, node.op == TokenType.SLASH
                        ? "division by constant zero" : "remainder by constant zero");
            }
        }
    }
}
