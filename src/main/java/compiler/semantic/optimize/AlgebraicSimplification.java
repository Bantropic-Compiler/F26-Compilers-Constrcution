package compiler.semantic.optimize;

import compiler.ast.Program;
import compiler.ast.expression.Expression;
import compiler.ast.expression.ModifiablePrimaryNode;
import compiler.ast.expression.expression.BinaryExpression;
import compiler.ast.expression.expression.RoutineCallExpression;
import compiler.ast.expression.literal.IntegerLiteral;
import compiler.ast.expression.literal.RealLiteral;
import compiler.lexer.TokenType;
import compiler.semantic.ConstantEvaluator;
import compiler.semantic.SemanticModel;
import compiler.semantic.type.PrimitiveType;
import compiler.semantic.visitor.AstRewriter;
import compiler.semantic.visitor.AstVisitor;

/** O3: type-preserving identities, without calls or discarded evaluation errors. */
public final class AlgebraicSimplification extends AstRewriter implements Optimization {
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
        // Inspect original nodes while their semantic information is still valid.
        Expression replacement = simplify(node);
        if (replacement == null) return super.visitBinary(node);
        markChanged();
        return rewriteExpression(replacement);
    }

    private Expression simplify(BinaryExpression node) {
        Expression replacement = simplifyOperands(node, node.left, node.right);
        if (replacement != null) return replacement;
        if (node.op == TokenType.PLUS || node.op == TokenType.STAR)
            return simplifyOperands(node, node.right, node.left);
        return null;
    }

    private Expression simplifyOperands(BinaryExpression node, Expression x, Expression constant) {
        var type = model.typeOf(node);
        if (!type.isNumeric() || type != model.typeOf(x) || containsCall(x)) return null;
        boolean integer = type == PrimitiveType.INTEGER;
        return switch (node.op) {
            // Adding +0 can turn real -0 into +0; retain real addition.
            case PLUS -> integer && isNumber(constant, 0) ? x : null;
            case MINUS -> isNumber(constant, 0) ? x : null;
            case STAR -> {
                if (isNumber(constant, 1)) yield x;
                // Real multiplication by zero can preserve a sign or produce NaN.
                if (integer && isNumber(constant, 0) && safeToDiscard(x))
                    yield new IntegerLiteral(node.line, node.column, 0);
                yield null;
            }
            case SLASH -> isNumber(constant, 1) ? x : null;
            default -> null;
        };
    }

    private static boolean isNumber(Expression expression, int value) {
        if (expression instanceof IntegerLiteral n) return n.value == value;
        if (expression instanceof RealLiteral n)
            return Double.doubleToRawLongBits(n.value) == Double.doubleToRawLongBits((double) value);
        return false;
    }

    private boolean safeToDiscard(Expression expression) {
        // Accesses and nonconstant arithmetic may fail at runtime. Keep those evaluations.
        if (expression instanceof ModifiablePrimaryNode n) return n.accessors.isEmpty();
        return ConstantEvaluator.valueOf(expression, model) != null;
    }

    private static boolean containsCall(Expression expression) {
        CallFinder finder = new CallFinder();
        finder.visit(expression);
        return finder.found;
    }

    private static final class CallFinder extends AstVisitor {
        private boolean found;

        @Override
        protected void visitRoutineCallExpression(RoutineCallExpression node) {
            found = true;
        }
    }
}
