package compiler.semantic.check;

import compiler.ast.BlockNode;
import compiler.ast.Node;
import compiler.ast.Program;
import compiler.ast.declaration.RoutineDeclaration;
import compiler.ast.statement.IfStatement;
import compiler.ast.statement.loop.BreakStatement;
import compiler.ast.statement.loop.ContinueStatement;
import compiler.ast.statement.loop.ForStatement;
import compiler.ast.statement.loop.WhileStatement;
import compiler.ast.statement.routine.ReturnStatement;
import compiler.semantic.Diagnostics;
import compiler.semantic.SemanticModel;
import compiler.semantic.visitor.AstVisitor;
import java.util.EnumSet;

/** C6: structural return-path analysis; loops may execute zero times. */
public final class ReturnPathChecker extends AstVisitor implements Check {
    private enum Exit { NEXT, RETURN, BREAK, CONTINUE }

    private Diagnostics diagnostics;

    @Override
    public void run(Program program, SemanticModel model, Diagnostics diagnostics) {
        this.diagnostics = diagnostics;
        visit(program);
    }

    @Override
    protected void visitRoutineDeclaration(RoutineDeclaration node) {
        if (node.returnType != null && node.body != null
                && !exits(node.body).equals(EnumSet.of(Exit.RETURN))) {
            diagnostics.report(node, "function '" + node.name + "' does not return a value on all paths");
        }
        super.visitRoutineDeclaration(node);
    }

    private EnumSet<Exit> exits(Node node) {
        if (node instanceof ReturnStatement) {
            // C1 checks missing values, C3 checks their types. Neither falls through.
            return EnumSet.of(Exit.RETURN);
        }
        if (node instanceof BreakStatement) {
            return EnumSet.of(Exit.BREAK);
        }
        if (node instanceof ContinueStatement) {
            return EnumSet.of(Exit.CONTINUE);
        }
        if (node instanceof BlockNode block) {
            EnumSet<Exit> result = EnumSet.of(Exit.NEXT);
            for (Node item : block.items) {
                if (!result.remove(Exit.NEXT)) {
                    break; // Later statements are unreachable on every remaining path.
                }
                result.addAll(exits(item));
            }
            return result;
        }
        if (node instanceof IfStatement conditional) {
            EnumSet<Exit> result = exits(conditional.thenBody);
            result.addAll(exits(conditional.elseBody));
            return result;
        }
        if (node instanceof WhileStatement loop) {
            return loopExits(loop.body);
        }
        if (node instanceof ForStatement loop) {
            return loopExits(loop.body);
        }
        return EnumSet.of(Exit.NEXT);
    }

    private EnumSet<Exit> loopExits(BlockNode body) {
        EnumSet<Exit> result = EnumSet.of(Exit.NEXT);
        if (exits(body).contains(Exit.RETURN)) {
            result.add(Exit.RETURN);
        }
        return result;
    }
}
