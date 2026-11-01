package compiler.semantic.check;

import compiler.ast.Program;
import compiler.ast.declaration.Parameter;
import compiler.ast.declaration.VariableDeclaration;
import compiler.ast.expression.Accessor;
import compiler.ast.expression.Expression;
import compiler.ast.expression.ModifiablePrimaryNode;
import compiler.ast.expression.accessor.FieldAccess;
import compiler.ast.expression.accessor.IndexAccess;
import compiler.ast.expression.expression.BinaryExpression;
import compiler.ast.statement.loop.ForStatement;
import compiler.ast.type.ArrayTypeNode;
import compiler.lexer.TokenType;
import compiler.semantic.ConstantEvaluator;
import compiler.semantic.Diagnostics;
import compiler.semantic.SemanticModel;
import compiler.semantic.symbol.VariableSymbol;
import compiler.semantic.type.ArrayType;
import compiler.semantic.type.PrimitiveType;
import compiler.semantic.type.RecordType;
import compiler.semantic.type.SemanticType;
import compiler.semantic.visitor.AstVisitor;
import java.util.IdentityHashMap;
import java.util.Map;

/** C4: array sizes and statically known index ranges (indices are one-based). */
public final class ArrayBoundsChecker extends AstVisitor implements Check {
    private record Range(long min, long max) {}

    private final Map<VariableSymbol, Range> ranges = new IdentityHashMap<>();
    private SemanticModel model;
    private Diagnostics diagnostics;
    private boolean parameterType;

    @Override
    public void run(Program program, SemanticModel model, Diagnostics diagnostics) {
        this.model = model;
        this.diagnostics = diagnostics;
        parameterType = false;
        ranges.clear();
        visit(program);
    }

    @Override
    protected void visitParameter(Parameter node) {
        boolean outer = parameterType;
        parameterType = true;
        super.visitParameter(node);
        parameterType = outer;
    }

    @Override
    protected void visitVariableDeclaration(VariableDeclaration node) {
        // Record fields are declarations, even inside a parameter's type.
        boolean outer = parameterType;
        parameterType = false;
        super.visitVariableDeclaration(node);
        parameterType = outer;
    }

    @Override
    protected void visitArrayType(ArrayTypeNode node) {
        if (node.sizeExpr == null) {
            if (!parameterType) {
                diagnostics.report(node, "array size may be omitted only in a routine parameter");
            }
        } else {
            SemanticType type = model.typeOf(node.sizeExpr);
            if (!type.isError()) {
                Long size = ConstantEvaluator.integerValue(node.sizeExpr);
                if (type != PrimitiveType.INTEGER || size == null || size <= 0) {
                    diagnostics.report(node.sizeExpr, "array size must be a positive constant integer");
                }
            }
        }
        super.visitArrayType(node);
    }

    @Override
    protected void visitFor(ForStatement node) {
        visit(node.rangeStart);
        visit(node.rangeEnd);
        Long start = ConstantEvaluator.integerValue(node.rangeStart);
        Long end = ConstantEvaluator.integerValue(node.rangeEnd);
        VariableSymbol symbol = model.variableOf(node);
        if (symbol != null && start != null && end != null && start <= end
                && model.typeOf(node.rangeStart) == PrimitiveType.INTEGER
                && model.typeOf(node.rangeEnd) == PrimitiveType.INTEGER) {
            // reverse changes traversal order, not the set of values.
            ranges.put(symbol, new Range(start, end));
        }
        visit(node.body);
        ranges.remove(symbol);
    }

    @Override
    protected void visitModifiablePrimary(ModifiablePrimaryNode node) {
        VariableSymbol symbol = model.variableOf(node);
        SemanticType type = symbol == null ? null : symbol.type();
        for (Accessor accessor : node.accessors) {
            if (accessor instanceof IndexAccess index) {
                if (type instanceof ArrayType array) {
                    checkIndex(index.index, array);
                    type = array.elementType();
                } else {
                    type = null;
                }
            } else if (accessor instanceof FieldAccess field) {
                type = type instanceof RecordType record ? record.fieldType(field.name) : null;
            }
        }
        super.visitModifiablePrimary(node);
    }

    private void checkIndex(Expression index, ArrayType array) {
        if (!array.hasSize() || model.typeOf(index) != PrimitiveType.INTEGER) {
            return;
        }
        Range range = rangeOf(index);
        if (range != null && (range.min < 1 || range.max > array.size())) {
            String value = range.min == range.max ? String.valueOf(range.min)
                    : range.min + ".." + range.max;
            diagnostics.report(index, "array index " + value + " is outside bounds 1.." + array.size());
        }
    }

    private Range rangeOf(Expression expression) {
        Long constant = ConstantEvaluator.integerValue(expression);
        if (constant != null) {
            return new Range(constant, constant);
        }
        if (expression instanceof ModifiablePrimaryNode primary && primary.accessors.isEmpty()) {
            return ranges.get(model.variableOf(primary));
        }
        if (expression instanceof BinaryExpression binary
                && (binary.op == TokenType.PLUS || binary.op == TokenType.MINUS)) {
            Range left = rangeOf(binary.left);
            Long right = ConstantEvaluator.integerValue(binary.right);
            if (left != null && right != null) {
                long offset = binary.op == TokenType.PLUS ? right : -right;
                long min = left.min + offset;
                long max = left.max + offset;
                // Do not infer bounds through an overflowing integer operation.
                if (min >= Integer.MIN_VALUE && max <= Integer.MAX_VALUE) {
                    return new Range(min, max);
                }
            }
        }
        return null;
    }
}
