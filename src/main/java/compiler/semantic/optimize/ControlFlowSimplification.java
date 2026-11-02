package compiler.semantic.optimize;

import compiler.ast.BlockNode;
import compiler.ast.Node;
import compiler.ast.Program;
import compiler.ast.expression.Expression;
import compiler.ast.statement.IfStatement;
import compiler.ast.statement.loop.ForStatement;
import compiler.ast.statement.loop.WhileStatement;
import compiler.semantic.ConstantEvaluator;
import compiler.semantic.SemanticModel;
import compiler.semantic.type.PrimitiveType;
import compiler.semantic.visitor.AstRewriter;
import compiler.semantic.visitor.AstVisitor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** O4: select constant branches and remove loops that cannot execute. */
public final class ControlFlowSimplification extends AstRewriter implements Optimization {
    private SemanticModel model;
    private final Map<String, Integer> declarationCounts = new HashMap<>();

    @Override
    public boolean run(Program program, SemanticModel model) {
        this.model = model;
        declarationCounts.clear();
        new DeclarationCounter().visit(program);
        resetChanged();
        rewrite(program);
        return changed();
    }

    @Override
    protected List<Node> rewriteItem(Node item) {
        if (item instanceof IfStatement conditional) {
            Boolean condition = booleanValue(conditional.condition);
            if (condition != null) {
                BlockNode selected = condition ? conditional.thenBody : conditional.elseBody;
                if (selected == null) return remove();
                if (canInline(selected)) {
                    markChanged();
                    // Defer nested transformations until the model has been refreshed.
                    return List.copyOf(selected.items);
                }
            }
        }
        if (item instanceof WhileStatement loop && Boolean.FALSE.equals(booleanValue(loop.condition)))
            return remove();
        if (item instanceof ForStatement loop && loop.rangeEnd != null) {
            Long start = ConstantEvaluator.integerValue(loop.rangeStart);
            Long end = ConstantEvaluator.integerValue(loop.rangeEnd);
            // reverse changes traversal order, not the range's set of values.
            if (start != null && end != null && start > end) return remove();
        }
        return super.rewriteItem(item);
    }

    private List<Node> remove() {
        markChanged();
        return List.of();
    }

    private Boolean booleanValue(Expression expression) {
        var value = ConstantEvaluator.valueOf(expression, model);
        if (value == null) return null;
        if (value.type() == PrimitiveType.BOOLEAN) return (Boolean) value.value();
        if (value.type() == PrimitiveType.INTEGER) {
            long number = (Long) value.value();
            if (number == 0 || number == 1) return number == 1;
        }
        return null;
    }

    private boolean canInline(BlockNode block) {
        // A globally unique declaration name cannot collide or capture a reference
        // outside this branch. Repeated names are conservatively left scoped.
        for (Node item : block.items) {
            var symbol = model.symbolOf(item);
            if (symbol != null && symbol.declaration() == item
                    && declarationCounts.getOrDefault(symbol.name(), 0) > 1) return false;
        }
        return true;
    }

    private final class DeclarationCounter extends AstVisitor {
        @Override
        public void visit(Node node) {
            if (node == null) return;
            var symbol = model.symbolOf(node);
            if (symbol != null && symbol.declaration() == node)
                declarationCounts.merge(symbol.name(), 1, Integer::sum);
            super.visit(node);
        }
    }
}
