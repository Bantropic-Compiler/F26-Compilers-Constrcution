package compiler.semantic.optimize;

import compiler.ast.Program;
import compiler.ast.declaration.VariableDeclaration;
import compiler.ast.expression.Expression;
import compiler.ast.expression.ModifiablePrimaryNode;
import compiler.ast.expression.literal.*;
import compiler.ast.statement.AssignmentStatement;
import compiler.semantic.SemanticModel;
import compiler.semantic.symbol.VariableSymbol;
import compiler.semantic.type.PrimitiveType;
import compiler.semantic.visitor.AstRewriter;
import compiler.semantic.visitor.AstVisitor;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/** O2: propagate literal initializers of primitive variables with no writes. */
public final class ConstantPropagation extends AstRewriter implements Optimization {
    private SemanticModel model;
    private Map<VariableSymbol, Expression> constants;

    @Override
    public boolean run(Program program, SemanticModel model) {
        this.model = model;
        constants = new ConstantCollector(model).collect(program);
        resetChanged();
        rewrite(program);
        return changed();
    }

    @Override
    protected Expression visitModifiablePrimary(ModifiablePrimaryNode node) {
        super.visitModifiablePrimary(node);
        VariableSymbol symbol = model.variableOf(node);
        Expression value = constants.get(symbol);
        if (value == null || !node.accessors.isEmpty()) return node;

        Expression literal = convert(value, (PrimitiveType) symbol.type(), node.line, node.column);
        markChanged();
        return literal;
    }

    /** Collect candidates first, then exclude every variable assigned anywhere. */
    private static final class ConstantCollector extends AstVisitor {
        private final SemanticModel model;
        private final Map<VariableSymbol, Expression> constants = new IdentityHashMap<>();
        private final Set<VariableSymbol> written = Collections.newSetFromMap(new IdentityHashMap<>());

        ConstantCollector(SemanticModel model) {
            this.model = model;
        }

        Map<VariableSymbol, Expression> collect(Program program) {
            visit(program);
            written.forEach(constants::remove);
            return constants;
        }

        @Override
        protected void visitVariableDeclaration(VariableDeclaration node) {
            super.visitVariableDeclaration(node);
            VariableSymbol symbol = model.variableOf(node);
            if (symbol == null || !(symbol.type() instanceof PrimitiveType type)) return;
            if (symbol.kind() != VariableSymbol.Kind.LOCAL
                    && symbol.kind() != VariableSymbol.Kind.GLOBAL) return;

            Expression literal = convert(node.initializer, type, node.line, node.column);
            if (literal != null) constants.put(symbol, literal);
        }

        @Override
        protected void visitAssignment(AssignmentStatement node) {
            written.add(model.variableOf(node.target));
            super.visitAssignment(node);
        }
    }

    /** Apply assignment conversion, rather than copying the initializer's type. */
    private static Expression convert(Expression value, PrimitiveType target, int line, int column) {
        if (target == PrimitiveType.CHAR) {
            return value instanceof CharLiteral c ? new CharLiteral(line, column, c.codePoint) : null;
        }
        if (target == PrimitiveType.BOOLEAN) {
            if (value instanceof BooleanLiteral b) return new BooleanLiteral(line, column, b.value);
            if (value instanceof IntegerLiteral i && (i.value == 0 || i.value == 1))
                return new BooleanLiteral(line, column, i.value == 1);
            return null;
        }
        double number;
        if (value instanceof IntegerLiteral i) {
            if (i.value < Integer.MIN_VALUE || i.value > Integer.MAX_VALUE) return null;
            number = i.value;
        } else if (value instanceof RealLiteral r) number = r.value;
        else if (value instanceof BooleanLiteral b) number = b.value ? 1 : 0;
        else return null;
        if (!Double.isFinite(number)) return null;
        if (target == PrimitiveType.REAL) return new RealLiteral(line, column, number);
        // The language specifies nearest-integer rounding, but no tie-breaking rule.
        // Leave half-integers untouched until that rule is specified.
        if (value instanceof RealLiteral && Math.abs(number - Math.floor(number)) == 0.5) return null;
        double rounded = Math.rint(number);
        if (rounded < Integer.MIN_VALUE || rounded > Integer.MAX_VALUE) return null;
        return new IntegerLiteral(line, column, (long) rounded);
    }
}
