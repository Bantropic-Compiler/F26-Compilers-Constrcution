package compiler.semantic.optimize;

import compiler.ast.Node;
import compiler.ast.Program;
import compiler.ast.declaration.VariableDeclaration;
import compiler.ast.expression.Expression;
import compiler.ast.expression.ModifiablePrimaryNode;
import compiler.ast.statement.AssignmentStatement;
import compiler.semantic.ConstantEvaluator;
import compiler.semantic.SemanticModel;
import compiler.semantic.symbol.VariableSymbol;
import compiler.semantic.type.PrimitiveType;
import compiler.semantic.type.StringType;
import compiler.semantic.visitor.AstRewriter;
import compiler.semantic.visitor.AstVisitor;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** O6: remove unread scalar locals only when all their writes can be discarded. */
public final class UnusedVariableElimination extends AstRewriter implements Optimization {
    private SemanticModel model;
    private Set<VariableSymbol> removable;

    @Override
    public boolean run(Program program, SemanticModel model) {
        this.model = model;
        UsageCollector collector = new UsageCollector();
        collector.visit(program);
        removable = collector.candidates;
        removable.removeAll(collector.required);
        resetChanged();
        rewrite(program);
        return changed();
    }

    @Override
    protected Node visitVariableDeclaration(VariableDeclaration node) {
        if (removable.contains(model.variableOf(node))) {
            markChanged();
            return null;
        }
        return super.visitVariableDeclaration(node);
    }

    @Override
    protected Node visitAssignment(AssignmentStatement node) {
        if (node.target.accessors.isEmpty() && removable.contains(model.variableOf(node.target))) {
            markChanged();
            return null;
        }
        return super.visitAssignment(node);
    }

    private boolean safeWrite(VariableSymbol symbol, Expression value) {
        if (value == null) return true;
        // Preserve potentially failing real-to-integer conversions, even for bare reads.
        if (symbol.type() == PrimitiveType.INTEGER && model.typeOf(value) == PrimitiveType.REAL)
            return false;
        if (value instanceof ModifiablePrimaryNode n) return n.accessors.isEmpty();
        return ConstantEvaluator.valueOf(value, model) != null;
    }

    private static Set<VariableSymbol> symbols() {
        return Collections.newSetFromMap(new IdentityHashMap<>());
    }

    private final class UsageCollector extends AstVisitor {
        private final Set<VariableSymbol> candidates = symbols();
        private final Set<VariableSymbol> required = symbols();

        @Override
        protected void visitVariableDeclaration(VariableDeclaration node) {
            VariableSymbol symbol = model.variableOf(node);
            // Keep aggregates: their creation and field initializers may evaluate expressions.
            if (symbol != null && symbol.kind() == VariableSymbol.Kind.LOCAL
                    && (symbol.type() instanceof PrimitiveType || symbol.type() == StringType.INSTANCE)) {
                candidates.add(symbol);
                if (!safeWrite(symbol, node.initializer)) required.add(symbol);
            }
            super.visitVariableDeclaration(node);
        }

        @Override
        protected void visitModifiablePrimary(ModifiablePrimaryNode node) {
            required.add(model.variableOf(node));
            super.visitModifiablePrimary(node);
        }

        @Override
        protected void visitAssignment(AssignmentStatement node) {
            VariableSymbol symbol = model.variableOf(node.target);
            if (!node.target.accessors.isEmpty()) visit(node.target);
            if (symbol != null && !safeWrite(symbol, node.value)) required.add(symbol);
            // A direct assignment target is a write; its right-hand side still contains reads.
            visit(node.value);
        }
    }
}
