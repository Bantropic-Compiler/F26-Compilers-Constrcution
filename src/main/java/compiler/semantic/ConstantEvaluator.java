package compiler.semantic;

import compiler.ast.expression.Expression;
import compiler.ast.expression.expression.BinaryExpression;
import compiler.ast.expression.expression.UnaryExpression;
import compiler.ast.expression.literal.IntegerLiteral;
import compiler.ast.expression.literal.RealLiteral;
import compiler.semantic.type.PrimitiveType;

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

    /** Numeric constant value, preserving integer division and rejecting overflow. */
    public static Double numericValue(Expression expression, SemanticModel model) {
        if (!model.typeOf(expression).isNumeric()) {
            return null;
        }
        Double value;
        if (expression instanceof IntegerLiteral literal) {
            value = (double) literal.value;
        } else if (expression instanceof RealLiteral literal) {
            value = literal.value;
        } else if (expression instanceof UnaryExpression unary) {
            Double operand = numericValue(unary.operand, model);
            if (operand == null) {
                return null;
            }
            value = switch (unary.op) {
                case PLUS -> operand;
                case MINUS -> -operand;
                default -> null;
            };
        } else if (expression instanceof BinaryExpression binary) {
            Double left = numericValue(binary.left, model);
            Double right = numericValue(binary.right, model);
            if (left == null || right == null) {
                return null;
            }
            value = switch (binary.op) {
                case PLUS -> left + right;
                case MINUS -> left - right;
                case STAR -> left * right;
                case SLASH -> right == 0.0 ? null : left / right;
                case PERCENT -> right == 0.0 ? null : left % right;
                default -> null;
            };
        } else {
            return null;
        }
        if (value == null || !Double.isFinite(value)) {
            return null;
        }
        if (model.typeOf(expression) == PrimitiveType.INTEGER) {
            value = value < 0 ? Math.ceil(value) : Math.floor(value);
            if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
                return null;
            }
        }
        return value;
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
