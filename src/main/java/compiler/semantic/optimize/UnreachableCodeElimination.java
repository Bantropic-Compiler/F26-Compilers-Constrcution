package compiler.semantic.optimize;

import compiler.ast.BlockNode;
import compiler.ast.Node;
import compiler.ast.Program;
import compiler.ast.statement.IfStatement;
import compiler.ast.statement.loop.BreakStatement;
import compiler.ast.statement.loop.ContinueStatement;
import compiler.ast.statement.routine.ReturnStatement;
import compiler.semantic.SemanticModel;
import compiler.semantic.visitor.AstRewriter;

/** O5: remove the rest of a block after an unconditional transfer of control. */
public final class UnreachableCodeElimination extends AstRewriter implements Optimization {
    @Override
    public boolean run(Program program, SemanticModel model) {
        resetChanged();
        rewrite(program);
        return changed();
    }

    @Override
    protected BlockNode visitBlock(BlockNode node) {
        super.visitBlock(node);
        for (int i = 0; i < node.items.size(); i++) {
            Node item = node.items.get(i);
            if (item instanceof BreakStatement || item instanceof ContinueStatement || alwaysReturns(item)) {
                if (i + 1 < node.items.size()) {
                    node.items.subList(i + 1, node.items.size()).clear();
                    markChanged();
                }
                break;
            }
        }
        return node;
    }

    private static boolean alwaysReturns(Node node) {
        if (node instanceof ReturnStatement) return true;
        if (node instanceof IfStatement conditional)
            return alwaysReturns(conditional.thenBody) && alwaysReturns(conditional.elseBody);
        // Nested blocks have already had their unreachable suffix removed.
        if (node instanceof BlockNode block && !block.items.isEmpty())
            return alwaysReturns(block.items.get(block.items.size() - 1));
        return false;
    }
}
