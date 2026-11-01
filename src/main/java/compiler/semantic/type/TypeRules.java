package compiler.semantic.type;

import compiler.ast.expression.Expression;
import compiler.semantic.ConstantEvaluator;

public final class TypeRules {

    private TypeRules() {
    }

    /** Receiver <- source; the same rules apply to initializers, assignments, arguments and return. */
    public static boolean assignable(SemanticType target, SemanticType source, Expression sourceExpression) {
        if (target.isError() || source.isError()) {
            return true;
        }
        if (target.isVoid() || source.isVoid()) {
            return false;
        }
        if (target == PrimitiveType.INTEGER || target == PrimitiveType.REAL) {
            return source.isNumeric() || source == PrimitiveType.BOOLEAN;
        }
        if (target == PrimitiveType.BOOLEAN) {
            return source == PrimitiveType.BOOLEAN
                    || (source == PrimitiveType.INTEGER && isBooleanConstant(sourceExpression));
        }
        if (target == PrimitiveType.CHAR) {
            return source == PrimitiveType.CHAR;
        }
        if (target == StringType.INSTANCE) {
            return source == StringType.INSTANCE;
        }
        if (target instanceof RecordType) {
            return target == source;
        }
        if (target instanceof ArrayType targetArray) {
            return source instanceof ArrayType sourceArray && arrayConforms(targetArray, sourceArray);
        }
        return false;
    }

    public static boolean arrayConforms(ArrayType target, ArrayType source) {
        if (target.hasSize() && !(source.hasSize() && source.size() == target.size())) {
            return false;
        }
        SemanticType targetElement = target.elementType();
        SemanticType sourceElement = source.elementType();
        if (targetElement.isError() || sourceElement.isError()) {
            return true;
        }
        if (targetElement instanceof ArrayType nestedTarget && sourceElement instanceof ArrayType nestedSource) {
            return arrayConforms(nestedTarget, nestedSource);
        }
        return targetElement.equals(sourceElement);
    }

    /** True for boolean and for the integer constants 0 and 1. */
    public static boolean conformsToBoolean(SemanticType type, Expression expression) {
        return type == PrimitiveType.BOOLEAN
                || (type == PrimitiveType.INTEGER && isBooleanConstant(expression));
    }

    private static boolean isBooleanConstant(Expression expression) {
        if (expression == null) {
            return false;
        }
        Long value = ConstantEvaluator.integerValue(expression);
        return value != null && (value == 0 || value == 1);
    }
}
