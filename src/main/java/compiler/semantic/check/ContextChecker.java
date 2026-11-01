package compiler.semantic.check;

import compiler.ast.Program;
import compiler.ast.declaration.RoutineDeclaration;
import compiler.ast.statement.loop.BreakStatement;
import compiler.ast.statement.loop.ContinueStatement;
import compiler.ast.statement.loop.ForStatement;
import compiler.ast.statement.loop.WhileStatement;
import compiler.ast.statement.routine.ReturnStatement;
import compiler.semantic.Diagnostics;
import compiler.semantic.SemanticModel;
import compiler.semantic.visitor.AstVisitor;

/** C1: checks keyword context; return value types are checked by TypeChecker. */
public final class ContextChecker extends AstVisitor implements Check {
    private Diagnostics diagnostics;
    private RoutineDeclaration currentRoutine;
    private int loopDepth;

    @Override
    public void run(Program program, SemanticModel model, Diagnostics diagnostics) {
        this.diagnostics = diagnostics;
        currentRoutine = null;
        loopDepth = 0;
        visit(program);
    }

    @Override
    protected void visitRoutineDeclaration(RoutineDeclaration node) {
        RoutineDeclaration outerRoutine = currentRoutine;
        int outerDepth = loopDepth;
        currentRoutine = node;
        loopDepth = 0;
        super.visitRoutineDeclaration(node);
        currentRoutine = outerRoutine;
        loopDepth = outerDepth;
    }

    @Override
    protected void visitWhile(WhileStatement node) {
        loopDepth++;
        super.visitWhile(node);
        loopDepth--;
    }

    @Override
    protected void visitFor(ForStatement node) {
        loopDepth++;
        super.visitFor(node);
        loopDepth--;
    }

    @Override
    protected void visitBreak(BreakStatement node) {
        if (loopDepth == 0) {
            diagnostics.report(node, "break is only allowed inside a loop");
        }
    }

    @Override
    protected void visitContinue(ContinueStatement node) {
        if (loopDepth == 0) {
            diagnostics.report(node, "continue is only allowed inside a loop");
        }
    }

    @Override
    protected void visitReturn(ReturnStatement node) {
        if (currentRoutine == null) {
            diagnostics.report(node, "return is only allowed inside a routine");
        } else if (currentRoutine.returnType == null && node.value != null) {
            diagnostics.report(node, "procedure cannot return a value");
        } else if (currentRoutine.returnType != null && node.value == null) {
            diagnostics.report(node, "function must return a value");
        }
        super.visitReturn(node);
    }
}
