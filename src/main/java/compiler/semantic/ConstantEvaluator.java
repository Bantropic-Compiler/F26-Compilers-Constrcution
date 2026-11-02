package compiler.semantic;

import compiler.ast.expression.Expression;
import compiler.ast.expression.expression.BinaryExpression;
import compiler.ast.expression.expression.UnaryExpression;
import compiler.ast.expression.literal.IntegerLiteral;
import compiler.ast.expression.literal.RealLiteral;
import compiler.semantic.type.PrimitiveType;
import compiler.semantic.type.SemanticType;
import compiler.semantic.type.StringType;
import compiler.ast.expression.literal.*;

public final class ConstantEvaluator {

    private ConstantEvaluator() {
    }

    /** A typed constant; null from valueOf means evaluation is unsafe or impossible. */
    public record Constant(SemanticType type, Object value) {}

    public static Constant valueOf(Expression expression, SemanticModel model) {
        SemanticType type = model.typeOf(expression);
        if (type.isError()) return null;
        if (type.isNumeric()) {
            Double value = numericValue(expression, model);
            if (value == null) return null;
            if (type == PrimitiveType.INTEGER) return new Constant(type, value.longValue());
            return new Constant(type, value);
        }
        if (expression instanceof BooleanLiteral n) return new Constant(type, n.value);
        if (expression instanceof CharLiteral n) return new Constant(type, n.codePoint);
        if (expression instanceof StringLiteral n) return new Constant(type, n.value);
        if (expression instanceof UnaryExpression n) {
            Constant operand = valueOf(n.operand, model);
            return operand != null && n.op == compiler.lexer.TokenType.NOT
                    ? new Constant(type, !booleanValue(operand)) : null;
        }
        if (!(expression instanceof BinaryExpression n)) return null;
        Constant left = valueOf(n.left, model);
        Constant right = valueOf(n.right, model);
        if (left == null || right == null) return null;
        if (type == StringType.INSTANCE) {
            return new Constant(type, textValue(left) + textValue(right));
        }
        Object result;
        switch (n.op) {
            case AND -> result = booleanValue(left) & booleanValue(right);
            case OR -> result = booleanValue(left) | booleanValue(right);
            case XOR -> result = booleanValue(left) ^ booleanValue(right);
            case EQ, NEQ -> {
                boolean equal = left.type().isNumeric() && right.type().isNumeric()
                        ? ((Number) left.value()).doubleValue() == ((Number) right.value()).doubleValue()
                        : left.value().equals(right.value());
                result = n.op == compiler.lexer.TokenType.EQ ? equal : !equal;
            }
            case LT, LE, GT, GE -> {
                double l = ((Number) left.value()).doubleValue();
                double r = ((Number) right.value()).doubleValue();
                result = switch (n.op) {
                    case LT -> l < r;
                    case LE -> l <= r;
                    case GT -> l > r;
                    default -> l >= r;
                };
            }
            default -> { return null; }
        }
        return new Constant(type, result);
    }

    private static boolean booleanValue(Constant value) {
        return value.type() == PrimitiveType.BOOLEAN
                ? (Boolean) value.value() : ((Number) value.value()).longValue() == 1;
    }

    private static String textValue(Constant value) {
        return value.type() == PrimitiveType.CHAR
                ? new String(Character.toChars((Integer) value.value())) : (String) value.value();
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
            if (unary.op == compiler.lexer.TokenType.MINUS
                    && unary.operand instanceof IntegerLiteral literal
                    && literal.value == 2147483648L) return (double) Integer.MIN_VALUE;
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
            if (unary.op == compiler.lexer.TokenType.MINUS
                    && unary.operand instanceof IntegerLiteral literal
                    && literal.value == 2147483648L) return (long) Integer.MIN_VALUE;
            Long operand = integerValue(unary.operand);
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
            Long left = integerValue(binary.left);
            Long right = integerValue(binary.right);
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
