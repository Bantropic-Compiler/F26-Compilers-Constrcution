package compiler.semantic.visitor;

import compiler.ast.BlockNode;
import compiler.ast.Node;
import compiler.ast.Program;
import compiler.ast.declaration.Declaration;
import compiler.ast.declaration.Parameter;
import compiler.ast.declaration.RoutineDeclaration;
import compiler.ast.declaration.TypeDeclaration;
import compiler.ast.declaration.VariableDeclaration;
import compiler.ast.expression.Accessor;
import compiler.ast.expression.Expression;
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
import compiler.ast.type.TypeNode;
import compiler.ast.type.TypeReferenceNode;
import java.util.ArrayList;
import java.util.List;

public abstract class AstRewriter {

    private boolean changed;

    public final boolean changed() {
        return changed;
    }

    public final void resetChanged() {
        changed = false;
    }

    protected final void markChanged() {
        changed = true;
    }

    public Program rewrite(Program program) {
        return (Program) rewriteNode(program);
    }

    protected final Node rewriteNode(Node node) {
        if (node == null) {
            return null;
        }
        if (node instanceof Program n) {
            return visitProgram(n);
        } else if (node instanceof BlockNode n) {
            return visitBlock(n);
        } else if (node instanceof VariableDeclaration n) {
            return visitVariableDeclaration(n);
        } else if (node instanceof TypeDeclaration n) {
            return visitTypeDeclaration(n);
        } else if (node instanceof RoutineDeclaration n) {
            return visitRoutineDeclaration(n);
        } else if (node instanceof Parameter n) {
            return visitParameter(n);
        } else if (node instanceof AssignmentStatement n) {
            return visitAssignment(n);
        } else if (node instanceof PrintStatement n) {
            return visitPrint(n);
        } else if (node instanceof IfStatement n) {
            return visitIf(n);
        } else if (node instanceof ReturnStatement n) {
            return visitReturn(n);
        } else if (node instanceof RoutineCallStatement n) {
            return visitRoutineCallStatement(n);
        } else if (node instanceof WhileStatement n) {
            return visitWhile(n);
        } else if (node instanceof ForStatement n) {
            return visitFor(n);
        } else if (node instanceof BreakStatement n) {
            return visitBreak(n);
        } else if (node instanceof ContinueStatement n) {
            return visitContinue(n);
        } else if (node instanceof ModifiablePrimaryNode n) {
            return visitModifiablePrimary(n);
        } else if (node instanceof BinaryExpression n) {
            return visitBinary(n);
        } else if (node instanceof UnaryExpression n) {
            return visitUnary(n);
        } else if (node instanceof RoutineCallExpression n) {
            return visitRoutineCallExpression(n);
        } else if (node instanceof IntegerLiteral n) {
            return visitIntegerLiteral(n);
        } else if (node instanceof RealLiteral n) {
            return visitRealLiteral(n);
        } else if (node instanceof BooleanLiteral n) {
            return visitBooleanLiteral(n);
        } else if (node instanceof CharLiteral n) {
            return visitCharLiteral(n);
        } else if (node instanceof StringLiteral n) {
            return visitStringLiteral(n);
        } else if (node instanceof FieldAccess n) {
            return visitFieldAccess(n);
        } else if (node instanceof IndexAccess n) {
            return visitIndexAccess(n);
        } else if (node instanceof PrimitiveTypeNode n) {
            return visitPrimitiveType(n);
        } else if (node instanceof StringTypeNode n) {
            return visitStringType(n);
        } else if (node instanceof ArrayTypeNode n) {
            return visitArrayType(n);
        } else if (node instanceof RecordTypeNode n) {
            return visitRecordType(n);
        } else if (node instanceof TypeReferenceNode n) {
            return visitTypeReference(n);
        }
        throw new IllegalArgumentException("unknown node: " + node.getClass().getName());
    }

    protected final Expression rewriteExpression(Expression expression) {
        return (Expression) rewriteNode(expression);
    }

    protected final <T extends Node> T rewriteInPlace(T node) {
        if (node == null) {
            return null;
        }
        Node result = rewriteNode(node);
        if (result != node) {
            throw new IllegalStateException(node.getClass().getSimpleName() + " at " + node.line + ":"
                    + node.column + " cannot be replaced at this position");
        }
        return node;
    }

    protected void rewriteExpressions(List<Expression> expressions) {
        for (int i = 0; i < expressions.size(); i++) {
            expressions.set(i, rewriteExpression(expressions.get(i)));
        }
    }

    protected void rewriteItems(List<Node> items) {
        List<Node> result = new ArrayList<>();
        for (Node item : items) {
            result.addAll(rewriteItem(item));
        }
        items.clear();
        items.addAll(result);
    }

    protected List<Node> rewriteItem(Node item) {
        Node replacement = rewriteNode(item);
        return replacement == null ? List.of() : List.of(replacement);
    }

    protected Program visitProgram(Program node) {
        List<Declaration> result = new ArrayList<>();
        for (Declaration declaration : node.declarations) {
            Node replacement = rewriteNode(declaration);
            if (replacement != null) {
                result.add((Declaration) replacement);
            }
        }
        node.declarations.clear();
        node.declarations.addAll(result);
        return node;
    }

    protected BlockNode visitBlock(BlockNode node) {
        rewriteItems(node.items);
        return node;
    }

    protected Node visitVariableDeclaration(VariableDeclaration node) {
        rewriteInPlace(node.type);
        node.initializer = rewriteExpression(node.initializer);
        return node;
    }

    protected Node visitTypeDeclaration(TypeDeclaration node) {
        rewriteInPlace(node.type);
        return node;
    }

    protected Node visitRoutineDeclaration(RoutineDeclaration node) {
        for (Parameter parameter : node.parameters) {
            rewriteInPlace(parameter);
        }
        rewriteInPlace(node.returnType);
        rewriteInPlace(node.body);
        return node;
    }

    protected Parameter visitParameter(Parameter node) {
        rewriteInPlace(node.type);
        return node;
    }

    protected Node visitAssignment(AssignmentStatement node) {
        rewriteInPlace(node.target);
        node.value = rewriteExpression(node.value);
        return node;
    }

    protected Node visitPrint(PrintStatement node) {
        rewriteExpressions(node.args);
        return node;
    }

    protected Node visitIf(IfStatement node) {
        node.condition = rewriteExpression(node.condition);
        rewriteInPlace(node.thenBody);
        rewriteInPlace(node.elseBody);
        return node;
    }

    protected Node visitReturn(ReturnStatement node) {
        node.value = rewriteExpression(node.value);
        return node;
    }

    protected Node visitRoutineCallStatement(RoutineCallStatement node) {
        rewriteExpressions(node.args);
        return node;
    }

    protected Node visitWhile(WhileStatement node) {
        node.condition = rewriteExpression(node.condition);
        rewriteInPlace(node.body);
        return node;
    }

    protected Node visitFor(ForStatement node) {
        node.rangeStart = rewriteExpression(node.rangeStart);
        node.rangeEnd = rewriteExpression(node.rangeEnd);
        rewriteInPlace(node.body);
        return node;
    }

    protected Node visitBreak(BreakStatement node) {
        return node;
    }

    protected Node visitContinue(ContinueStatement node) {
        return node;
    }

    protected Expression visitModifiablePrimary(ModifiablePrimaryNode node) {
        for (Accessor accessor : node.accessors) {
            rewriteInPlace(accessor);
        }
        return node;
    }

    protected Expression visitBinary(BinaryExpression node) {
        node.left = rewriteExpression(node.left);
        node.right = rewriteExpression(node.right);
        return node;
    }

    protected Expression visitUnary(UnaryExpression node) {
        node.operand = rewriteExpression(node.operand);
        return node;
    }

    protected Expression visitRoutineCallExpression(RoutineCallExpression node) {
        rewriteExpressions(node.args);
        return node;
    }

    protected Expression visitIntegerLiteral(IntegerLiteral node) {
        return node;
    }

    protected Expression visitRealLiteral(RealLiteral node) {
        return node;
    }

    protected Expression visitBooleanLiteral(BooleanLiteral node) {
        return node;
    }

    protected Expression visitCharLiteral(CharLiteral node) {
        return node;
    }

    protected Expression visitStringLiteral(StringLiteral node) {
        return node;
    }

    protected Accessor visitFieldAccess(FieldAccess node) {
        return node;
    }

    protected Accessor visitIndexAccess(IndexAccess node) {
        node.index = rewriteExpression(node.index);
        return node;
    }

    protected TypeNode visitPrimitiveType(PrimitiveTypeNode node) {
        return node;
    }

    protected TypeNode visitStringType(StringTypeNode node) {
        return node;
    }

    protected TypeNode visitArrayType(ArrayTypeNode node) {
        node.sizeExpr = rewriteExpression(node.sizeExpr);
        rewriteInPlace(node.elementType);
        return node;
    }

    protected TypeNode visitRecordType(RecordTypeNode node) {
        for (VariableDeclaration member : node.members) {
            rewriteInPlace(member);
        }
        return node;
    }

    protected TypeNode visitTypeReference(TypeReferenceNode node) {
        return node;
    }
}
