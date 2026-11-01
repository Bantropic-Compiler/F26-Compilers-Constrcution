package compiler.semantic;

import compiler.ast.expression.Expression;
import compiler.ast.expression.expression.BinaryExpression;
import compiler.ast.expression.expression.UnaryExpression;
import compiler.ast.expression.literal.IntegerLiteral;

public final class ConstantEvaluator {

    private ConstantEvaluator() {
    }

    /** Value of a constant integer expression built from literals, or null if it is not one. */
    public static Long integerValue(Expression expression) {
        try {
            Long value = evaluate(expression);
            if (value == null || value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
                return null;
            }
            return value;
        } catch (ArithmeticException overflow) {
            return null;
        }
    }

    private static Long evaluate(Expression expression) {
        if (expression instanceof IntegerLiteral literal) {
            return literal.value;
        }
        if (expression instanceof UnaryExpression unary) {
            Long operand = evaluate(unary.operand);
            if (operand == null) {
                return null;
            }
            return switch (unary.op) {
                case PLUS -> operand;
                case MINUS -> Math.negateExact(operand);
                default -> null;
            };
        }
        if (expression instanceof BinaryExpression binary) {
            Long left = evaluate(binary.left);
            Long right = evaluate(binary.right);
            if (left == null || right == null) {
                return null;
            }
            long l = left;
            long r = right;
            return switch (binary.op) {
                case PLUS -> Math.addExact(l, r);
                case MINUS -> Math.subtractExact(l, r);
                case STAR -> Math.multiplyExact(l, r);
                case SLASH -> r == 0 ? null : Long.valueOf(l / r);
                case PERCENT -> r == 0 ? null : Long.valueOf(l % r);
                default -> null;
            };
        }
        return null;
    }
}
