package compiler.semantic.visitor;

import compiler.ast.BlockNode;
import compiler.ast.Node;
import compiler.ast.Program;
import compiler.ast.declaration.Parameter;
import compiler.ast.declaration.RoutineDeclaration;
import compiler.ast.declaration.TypeDeclaration;
import compiler.ast.declaration.VariableDeclaration;
import compiler.ast.expression.ModifiablePrimaryNode;
import compiler.ast.expression.accessor.FieldAccess;
import compiler.ast.expression.accessor.IndexAccess;
import compiler.ast.expression.expression.BinaryExpression;
import compiler.ast.expression.expression.RoutineCallExpression;
import compiler.ast.expression.expression.UnaryExpression;
import compiler.ast.expression.literal.BooleanLiteral;
import compiler.ast.expression.literal.CharLiteral;
import compiler.ast.expression.literal.IntegerLiteral;
import compiler.ast.expression.literal.RealLiteral;
import compiler.ast.expression.literal.StringLiteral;
import compiler.ast.statement.AssignmentStatement;
import compiler.ast.statement.IfStatement;
import compiler.ast.statement.PrintStatement;
import compiler.ast.statement.loop.BreakStatement;
import compiler.ast.statement.loop.ContinueStatement;
import compiler.ast.statement.loop.ForStatement;
import compiler.ast.statement.loop.WhileStatement;
import compiler.ast.statement.routine.ReturnStatement;
import compiler.ast.statement.routine.RoutineCallStatement;
import compiler.ast.type.ArrayTypeNode;
import compiler.ast.type.PrimitiveTypeNode;
import compiler.ast.type.RecordTypeNode;
import compiler.ast.type.StringTypeNode;
import compiler.ast.type.TypeReferenceNode;
import java.util.ArrayList;
import java.util.List;

public abstract class AstVisitor {

    public void visit(Node node) {
        if (node == null) {
            return;
        }
        if (node instanceof Program n) {
            visitProgram(n);
        } else if (node instanceof BlockNode n) {
            visitBlock(n);
        } else if (node instanceof VariableDeclaration n) {
            visitVariableDeclaration(n);
        } else if (node instanceof TypeDeclaration n) {
            visitTypeDeclaration(n);
        } else if (node instanceof RoutineDeclaration n) {
            visitRoutineDeclaration(n);
        } else if (node instanceof Parameter n) {
            visitParameter(n);
        } else if (node instanceof AssignmentStatement n) {
            visitAssignment(n);
        } else if (node instanceof PrintStatement n) {
            visitPrint(n);
        } else if (node instanceof IfStatement n) {
            visitIf(n);
        } else if (node instanceof ReturnStatement n) {
            visitReturn(n);
        } else if (node instanceof RoutineCallStatement n) {
            visitRoutineCallStatement(n);
        } else if (node instanceof WhileStatement n) {
            visitWhile(n);
        } else if (node instanceof ForStatement n) {
            visitFor(n);
        } else if (node instanceof BreakStatement n) {
            visitBreak(n);
        } else if (node instanceof ContinueStatement n) {
            visitContinue(n);
        } else if (node instanceof ModifiablePrimaryNode n) {
            visitModifiablePrimary(n);
        } else if (node instanceof BinaryExpression n) {
            visitBinary(n);
        } else if (node instanceof UnaryExpression n) {
            visitUnary(n);
        } else if (node instanceof RoutineCallExpression n) {
            visitRoutineCallExpression(n);
        } else if (node instanceof IntegerLiteral n) {
            visitIntegerLiteral(n);
        } else if (node instanceof RealLiteral n) {
            visitRealLiteral(n);
        } else if (node instanceof BooleanLiteral n) {
            visitBooleanLiteral(n);
        } else if (node instanceof CharLiteral n) {
            visitCharLiteral(n);
        } else if (node instanceof StringLiteral n) {
            visitStringLiteral(n);
        } else if (node instanceof FieldAccess n) {
            visitFieldAccess(n);
        } else if (node instanceof IndexAccess n) {
            visitIndexAccess(n);
        } else if (node instanceof PrimitiveTypeNode n) {
            visitPrimitiveType(n);
        } else if (node instanceof StringTypeNode n) {
            visitStringType(n);
        } else if (node instanceof ArrayTypeNode n) {
            visitArrayType(n);
        } else if (node instanceof RecordTypeNode n) {
            visitRecordType(n);
        } else if (node instanceof TypeReferenceNode n) {
            visitTypeReference(n);
        } else {
            throw new IllegalArgumentException("unknown node: " + node.getClass().getName());
        }
    }

    protected void visitAll(List<? extends Node> nodes) {
        for (Node node : new ArrayList<>(nodes)) {
            visit(node);
        }
    }

    protected void visitProgram(Program node) {
        visitAll(node.declarations);
    }

    protected void visitBlock(BlockNode node) {
        visitAll(node.items);
    }

    protected void visitVariableDeclaration(VariableDeclaration node) {
        visit(node.type);
        visit(node.initializer);
    }

    protected void visitTypeDeclaration(TypeDeclaration node) {
        visit(node.type);
    }

    protected void visitRoutineDeclaration(RoutineDeclaration node) {
        visitAll(node.parameters);
        visit(node.returnType);
        visit(node.body);
    }

    protected void visitParameter(Parameter node) {
        visit(node.type);
    }

    protected void visitAssignment(AssignmentStatement node) {
        visit(node.target);
        visit(node.value);
    }

    protected void visitPrint(PrintStatement node) {
        visitAll(node.args);
    }

    protected void visitIf(IfStatement node) {
        visit(node.condition);
        visit(node.thenBody);
        visit(node.elseBody);
    }

    protected void visitReturn(ReturnStatement node) {
        visit(node.value);
    }

    protected void visitRoutineCallStatement(RoutineCallStatement node) {
        visitAll(node.args);
    }

    protected void visitWhile(WhileStatement node) {
        visit(node.condition);
        visit(node.body);
    }

    protected void visitFor(ForStatement node) {
        visit(node.rangeStart);
        visit(node.rangeEnd);
        visit(node.body);
    }

    protected void visitBreak(BreakStatement node) {
    }

    protected void visitContinue(ContinueStatement node) {
    }

    protected void visitModifiablePrimary(ModifiablePrimaryNode node) {
        visitAll(node.accessors);
    }

    protected void visitBinary(BinaryExpression node) {
        visit(node.left);
        visit(node.right);
    }

    protected void visitUnary(UnaryExpression node) {
        visit(node.operand);
    }

    protected void visitRoutineCallExpression(RoutineCallExpression node) {
        visitAll(node.args);
    }

    protected void visitIntegerLiteral(IntegerLiteral node) {
    }

    protected void visitRealLiteral(RealLiteral node) {
    }

    protected void visitBooleanLiteral(BooleanLiteral node) {
    }

    protected void visitCharLiteral(CharLiteral node) {
    }

    protected void visitStringLiteral(StringLiteral node) {
    }

    protected void visitFieldAccess(FieldAccess node) {
    }

    protected void visitIndexAccess(IndexAccess node) {
        visit(node.index);
    }

    protected void visitPrimitiveType(PrimitiveTypeNode node) {
    }

    protected void visitStringType(StringTypeNode node) {
    }

    protected void visitArrayType(ArrayTypeNode node) {
        visit(node.sizeExpr);
        visit(node.elementType);
    }

    protected void visitRecordType(RecordTypeNode node) {
        visitAll(node.members);
    }

    protected void visitTypeReference(TypeReferenceNode node) {
    }
}
