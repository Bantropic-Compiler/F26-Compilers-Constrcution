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

public final class AstCopier {

    private AstCopier() {
    }

    @SuppressWarnings("unchecked")
    public static <T extends Node> T copy(T node) {
        return (T) copyNode(node);
    }

    private static <T extends Node> List<T> copyAll(List<T> nodes) {
        List<T> result = new ArrayList<>();
        for (T node : nodes) {
            result.add(copy(node));
        }
        return result;
    }

    private static Node copyNode(Node node) {
        if (node == null) {
            return null;
        }
        if (node instanceof Program n) {
            return new Program(n.line, n.column, copyAll(n.declarations));
        } else if (node instanceof BlockNode n) {
            return BlockNode.wrap(n.line, n.column, copyAll(n.items));
        } else if (node instanceof VariableDeclaration n) {
            return new VariableDeclaration(n.line, n.column, n.name, copy(n.type), copy(n.initializer));
        } else if (node instanceof TypeDeclaration n) {
            return new TypeDeclaration(n.line, n.column, n.name, copy(n.type));
        } else if (node instanceof RoutineDeclaration n) {
            return new RoutineDeclaration(n.line, n.column, n.name, copyAll(n.parameters),
                    copy(n.returnType), copy(n.body));
        } else if (node instanceof Parameter n) {
            return new Parameter(n.line, n.column, n.name, copy(n.type));
        } else if (node instanceof AssignmentStatement n) {
            return new AssignmentStatement(n.line, n.column, copy(n.target), copy(n.value));
        } else if (node instanceof PrintStatement n) {
            return new PrintStatement(n.line, n.column, copyAll(n.args));
        } else if (node instanceof IfStatement n) {
            return new IfStatement(n.line, n.column, copy(n.condition), copy(n.thenBody), copy(n.elseBody));
        } else if (node instanceof ReturnStatement n) {
            return ReturnStatement.wrap(n.line, n.column, copy(n.value));
        } else if (node instanceof RoutineCallStatement n) {
            return new RoutineCallStatement(n.line, n.column, n.name, copyAll(n.args));
        } else if (node instanceof WhileStatement n) {
            return new WhileStatement(n.line, n.column, copy(n.condition), copy(n.body));
        } else if (node instanceof ForStatement n) {
            return new ForStatement(n.line, n.column, n.loopVar, copy(n.rangeStart), copy(n.rangeEnd),
                    n.reverse, copy(n.body));
        } else if (node instanceof BreakStatement n) {
            return new BreakStatement(n.line, n.column);
        } else if (node instanceof ContinueStatement n) {
            return new ContinueStatement(n.line, n.column);
        } else if (node instanceof ModifiablePrimaryNode n) {
            return new ModifiablePrimaryNode(n.line, n.column, n.base, copyAll(n.accessors));
        } else if (node instanceof BinaryExpression n) {
            return new BinaryExpression(n.line, n.column, n.op, copy(n.left), copy(n.right));
        } else if (node instanceof UnaryExpression n) {
            return new UnaryExpression(n.line, n.column, n.op, copy(n.operand));
        } else if (node instanceof RoutineCallExpression n) {
            return new RoutineCallExpression(n.line, n.column, n.name, copyAll(n.args));
        } else if (node instanceof IntegerLiteral n) {
            return new IntegerLiteral(n.line, n.column, n.value);
        } else if (node instanceof RealLiteral n) {
            return new RealLiteral(n.line, n.column, n.value);
        } else if (node instanceof BooleanLiteral n) {
            return new BooleanLiteral(n.line, n.column, n.value);
        } else if (node instanceof CharLiteral n) {
            return new CharLiteral(n.line, n.column, n.codePoint);
        } else if (node instanceof StringLiteral n) {
            return new StringLiteral(n.line, n.column, n.value);
        } else if (node instanceof FieldAccess n) {
            return new FieldAccess(n.line, n.column, n.name);
        } else if (node instanceof IndexAccess n) {
            return new IndexAccess(n.line, n.column, copy(n.index));
        } else if (node instanceof PrimitiveTypeNode n) {
            return new PrimitiveTypeNode(n.line, n.column, n.kind);
        } else if (node instanceof StringTypeNode n) {
            return new StringTypeNode(n.line, n.column);
        } else if (node instanceof ArrayTypeNode n) {
            return new ArrayTypeNode(n.line, n.column, copy(n.sizeExpr), copy(n.elementType));
        } else if (node instanceof RecordTypeNode n) {
            return new RecordTypeNode(n.line, n.column, copyAll(n.members));
        } else if (node instanceof TypeReferenceNode n) {
            return new TypeReferenceNode(n.line, n.column, n.name);
        }
        throw new IllegalArgumentException("unknown node: " + node.getClass().getName());
    }
}
