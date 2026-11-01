package compiler.semantic.visitor;

import compiler.ast.BlockNode;
import compiler.ast.Node;
import compiler.ast.Program;
import compiler.ast.declaration.Declaration;
import compiler.ast.declaration.Parameter;
import compiler.ast.declaration.RoutineDeclaration;
import compiler.ast.declaration.TypeDeclaration;
import compiler.ast.declaration.VariableDeclaration;
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
import compiler.ast.type.TypeReferenceNode;
import compiler.semantic.Operators;
import compiler.lexer.TokenType;
import java.math.BigDecimal;
import java.util.List;

public final class SourcePrinter extends AstVisitor {

    private static final String INDENT = "    ";
    private static final int PRIMARY = 5;

    private final StringBuilder out = new StringBuilder();
    private int depth;

    private SourcePrinter() {
    }

    public static String print(Node node) {
        SourcePrinter printer = new SourcePrinter();
        printer.visit(node);
        return printer.out.toString();
    }

    private void indent() {
        out.append(INDENT.repeat(depth));
    }

    private void visitSeparated(List<? extends Node> nodes) {
        for (int i = 0; i < nodes.size(); i++) {
            if (i > 0) {
                out.append(", ");
            }
            visit(nodes.get(i));
        }
    }

    @Override
    protected void visitProgram(Program node) {
        List<Declaration> declarations = node.declarations;
        for (int i = 0; i < declarations.size(); i++) {
            if (i > 0 && (declarations.get(i) instanceof RoutineDeclaration
                    || declarations.get(i - 1) instanceof RoutineDeclaration)) {
                out.append('\n');
            }
            visit(declarations.get(i));
        }
    }

    @Override
    protected void visitBlock(BlockNode node) {
        depth++;
        visitAll(node.items);
        depth--;
    }

    @Override
    protected void visitVariableDeclaration(VariableDeclaration node) {
        indent();
        out.append("var ").append(node.name);
        if (node.type != null) {
            out.append(": ");
            visit(node.type);
        }
        if (node.initializer != null) {
            out.append(" is ");
            visit(node.initializer);
        }
        out.append('\n');
    }

    @Override
    protected void visitTypeDeclaration(TypeDeclaration node) {
        indent();
        out.append("type ").append(node.name).append(" is ");
        visit(node.type);
        out.append('\n');
    }

    @Override
    protected void visitRoutineDeclaration(RoutineDeclaration node) {
        indent();
        out.append("routine ").append(node.name).append('(');
        visitSeparated(node.parameters);
        out.append(')');
        if (node.returnType != null) {
            out.append(": ");
            visit(node.returnType);
        }
        if (node.body == null) {
            out.append('\n');
            return;
        }
        out.append(" is\n");
        visit(node.body);
        indent();
        out.append("end\n");
    }

    @Override
    protected void visitParameter(Parameter node) {
        out.append(node.name).append(": ");
        visit(node.type);
    }

    @Override
    protected void visitAssignment(AssignmentStatement node) {
        indent();
        visit(node.target);
        out.append(" := ");
        visit(node.value);
        out.append('\n');
    }

    @Override
    protected void visitPrint(PrintStatement node) {
        indent();
        out.append("print ");
        visitSeparated(node.args);
        out.append('\n');
    }

    @Override
    protected void visitIf(IfStatement node) {
        indent();
        out.append("if ");
        visit(node.condition);
        out.append(" then\n");
        visit(node.thenBody);
        if (node.elseBody != null) {
            indent();
            out.append("else\n");
            visit(node.elseBody);
        }
        indent();
        out.append("end\n");
    }

    @Override
    protected void visitReturn(ReturnStatement node) {
        indent();
        out.append("return");
        if (node.value != null) {
            out.append(' ');
            visit(node.value);
        }
        out.append('\n');
    }

    @Override
    protected void visitRoutineCallStatement(RoutineCallStatement node) {
        indent();
        out.append(node.name).append('(');
        visitSeparated(node.args);
        out.append(")\n");
    }

    @Override
    protected void visitWhile(WhileStatement node) {
        indent();
        out.append("while ");
        visit(node.condition);
        out.append(" loop\n");
        visit(node.body);
        indent();
        out.append("end\n");
    }

    @Override
    protected void visitFor(ForStatement node) {
        indent();
        out.append("for ").append(node.loopVar).append(" in ");
        visit(node.rangeStart);
        if (node.rangeEnd != null) {
            out.append("..");
            visit(node.rangeEnd);
        }
        if (node.reverse) {
            out.append(" reverse");
        }
        out.append(" loop\n");
        visit(node.body);
        indent();
        out.append("end\n");
    }

    @Override
    protected void visitBreak(BreakStatement node) {
        indent();
        out.append("break\n");
    }

    @Override
    protected void visitContinue(ContinueStatement node) {
        indent();
        out.append("continue\n");
    }

    @Override
    protected void visitModifiablePrimary(ModifiablePrimaryNode node) {
        out.append(node.base);
        visitAll(node.accessors);
    }

    @Override
    protected void visitFieldAccess(FieldAccess node) {
        out.append('.').append(node.name);
    }

    @Override
    protected void visitIndexAccess(IndexAccess node) {
        out.append('[');
        visit(node.index);
        out.append(']');
    }

    @Override
    protected void visitBinary(BinaryExpression node) {
        int precedence = Operators.precedence(node.op);
        boolean relation = precedence == 2;
        printOperand(node.left, relation ? precedence + 1 : precedence);
        out.append(' ').append(Operators.symbol(node.op)).append(' ');
        printOperand(node.right, precedence + 1);
    }

    @Override
    protected void visitUnary(UnaryExpression node) {
        out.append(Operators.symbol(node.op));
        if (node.op == TokenType.NOT) {
            out.append(' ');
        }
        printOperand(node.operand, PRIMARY);
    }

    @Override
    protected void visitRoutineCallExpression(RoutineCallExpression node) {
        out.append(node.name).append('(');
        visitSeparated(node.args);
        out.append(')');
    }

    @Override
    protected void visitIntegerLiteral(IntegerLiteral node) {
        out.append(node.value);
    }

    @Override
    protected void visitRealLiteral(RealLiteral node) {
        String text = BigDecimal.valueOf(node.value).toPlainString();
        if (text.indexOf('.') < 0) {
            text += ".0";
        }
        out.append(text);
    }

    @Override
    protected void visitBooleanLiteral(BooleanLiteral node) {
        out.append(node.value ? "true" : "false");
    }

    @Override
    protected void visitCharLiteral(CharLiteral node) {
        quote(new String(Character.toChars(node.codePoint)), '\'');
    }

    @Override
    protected void visitStringLiteral(StringLiteral node) {
        quote(node.value, '"');
    }

    @Override
    protected void visitPrimitiveType(PrimitiveTypeNode node) {
        out.append(node.kind.name().toLowerCase());
    }

    @Override
    protected void visitStringType(StringTypeNode node) {
        out.append("string");
    }

    @Override
    protected void visitArrayType(ArrayTypeNode node) {
        out.append("array[");
        visit(node.sizeExpr);
        out.append("] ");
        visit(node.elementType);
    }

    @Override
    protected void visitRecordType(RecordTypeNode node) {
        out.append("record\n");
        depth++;
        visitAll(node.members);
        depth--;
        indent();
        out.append("end");
    }

    @Override
    protected void visitTypeReference(TypeReferenceNode node) {
        out.append(node.name);
    }

    private void printOperand(Expression operand, int minimumLevel) {
        boolean parenthesize = level(operand) < minimumLevel;
        if (parenthesize) {
            out.append('(');
        }
        visit(operand);
        if (parenthesize) {
            out.append(')');
        }
    }

    private static int level(Expression expression) {
        if (expression instanceof BinaryExpression binary) {
            return Operators.precedence(binary.op);
        }
        return PRIMARY;
    }

    private void quote(String text, char delimiter) {
        out.append(delimiter);
        text.codePoints().forEach(codePoint -> {
            if (codePoint == '\n') {
                out.append("\\n");
            } else if (codePoint == '\t') {
                out.append("\\t");
            } else if (codePoint == '\\') {
                out.append("\\\\");
            } else if (codePoint == delimiter) {
                out.append('\\').append(delimiter);
            } else {
                out.appendCodePoint(codePoint);
            }
        });
        out.append(delimiter);
    }
}
