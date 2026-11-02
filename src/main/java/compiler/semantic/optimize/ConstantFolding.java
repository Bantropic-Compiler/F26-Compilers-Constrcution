package compiler.semantic.optimize;

import compiler.ast.Program;
import compiler.ast.expression.Expression;
import compiler.ast.expression.expression.BinaryExpression;
import compiler.ast.expression.expression.UnaryExpression;
import compiler.ast.expression.literal.*;
import compiler.semantic.ConstantEvaluator;
import compiler.semantic.SemanticModel;
import compiler.semantic.type.PrimitiveType;
import compiler.semantic.type.StringType;
import compiler.semantic.visitor.AstRewriter;

/** O1: fold literal expressions without discarding effects or unsafe arithmetic. */
public final class ConstantFolding extends AstRewriter implements Optimization {
    private SemanticModel model;

    @Override
    public boolean run(Program program, SemanticModel model) {
        this.model = model;
        resetChanged();
        rewrite(program);
        return changed();
    }

    @Override
    protected Expression visitBinary(BinaryExpression node) {
        // Evaluate before rewriting: the model still describes all original nodes.
        Expression folded = fold(node);
        return folded != null ? folded : super.visitBinary(node);
    }

    @Override
    protected Expression visitUnary(UnaryExpression node) {
        Expression folded = fold(node);
        return folded != null ? folded : super.visitUnary(node);
    }

    private Expression fold(Expression node) {
        var constant = ConstantEvaluator.valueOf(node, model);
        if (constant == null) return null;
        var type = constant.type();
        Object value = constant.value();
        Expression literal;
        if (type == PrimitiveType.INTEGER) literal = new IntegerLiteral(node.line, node.column, (Long) value);
        else if (type == PrimitiveType.REAL) literal = new RealLiteral(node.line, node.column, (Double) value);
        else if (type == PrimitiveType.BOOLEAN) literal = new BooleanLiteral(node.line, node.column, (Boolean) value);
        else if (type == PrimitiveType.CHAR) literal = new CharLiteral(node.line, node.column, (Integer) value);
        else if (type == StringType.INSTANCE) literal = new StringLiteral(node.line, node.column, (String) value);
        else return null;
        markChanged();
        return literal;
    }
}
