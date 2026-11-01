package compiler.semantic.check;

import compiler.ast.Program;
import compiler.ast.expression.Expression;
import compiler.ast.statement.AssignmentStatement;
import compiler.ast.statement.loop.ForStatement;
import compiler.semantic.Diagnostics;
import compiler.semantic.SemanticModel;
import compiler.semantic.symbol.VariableSymbol;
import compiler.semantic.type.ArrayType;
import compiler.semantic.type.PrimitiveType;
import compiler.semantic.type.SemanticType;
import compiler.semantic.visitor.AstVisitor;

/** C5: validates for sources and keeps loop variables read-only. */
public final class LoopChecker extends AstVisitor implements Check {
    private SemanticModel model;
    private Diagnostics diagnostics;

    @Override
    public void run(Program program, SemanticModel model, Diagnostics diagnostics) {
        this.model = model;
        this.diagnostics = diagnostics;
        visit(program);
    }

    @Override
    protected void visitFor(ForStatement node) {
        if (node.rangeEnd != null) {
            requireInteger(node.rangeStart);
            requireInteger(node.rangeEnd);
        } else {
            SemanticType type = model.typeOf(node.rangeStart);
            if (!type.isError() && !(type instanceof ArrayType)) {
                diagnostics.report(node.rangeStart,
                        "for iteration requires an array, found '" + type + "'");
            }
        }
        super.visitFor(node);
    }

    private void requireInteger(Expression bound) {
        SemanticType type = model.typeOf(bound);
        if (!type.isError() && type != PrimitiveType.INTEGER) {
            diagnostics.report(bound, "for range bound must be integer, found '" + type + "'");
        }
    }

    @Override
    protected void visitAssignment(AssignmentStatement node) {
        VariableSymbol symbol = model.variableOf(node.target);
        if (symbol != null && symbol.kind() == VariableSymbol.Kind.LOOP) {
            diagnostics.report(node.target, "cannot assign to loop variable '" + symbol.name() + "'");
        }
        super.visitAssignment(node);
    }
}
