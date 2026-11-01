package compiler.semantic.check;

import java.util.ArrayList;
import java.util.List;

import compiler.ast.Node;
import compiler.ast.Program;
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
import compiler.lexer.TokenType;
import compiler.semantic.ConstantEvaluator;
import compiler.semantic.Diagnostics;
import compiler.semantic.Operators;
import compiler.semantic.SemanticModel;
import compiler.semantic.symbol.RoutineSymbol;
import compiler.semantic.symbol.TypeSymbol;
import compiler.semantic.symbol.VariableSymbol;
import compiler.semantic.type.ArrayType;
import compiler.semantic.type.ErrorType;
import compiler.semantic.type.PrimitiveType;
import compiler.semantic.type.RecordType;
import compiler.semantic.type.SemanticType;
import compiler.semantic.type.StringType;
import compiler.semantic.type.TypeRules;
import compiler.semantic.type.VoidType;
import compiler.semantic.visitor.AstVisitor;

public final class TypeChecker extends AstVisitor implements Check {

    private SemanticModel model;
    private Diagnostics diagnostics;
    private RoutineSymbol currentRoutine;
    private String readOnlyReason;

    @Override
    public void run(Program program, SemanticModel model, Diagnostics diagnostics) {
        this.model = model;
        this.diagnostics = diagnostics;
        this.currentRoutine = null;
        this.readOnlyReason = null;
        visit(program);
    }

    @Override
    protected void visitVariableDeclaration(VariableDeclaration node) {
        resolveVariable(node);
    }

    @Override
    protected void visitTypeDeclaration(TypeDeclaration node) {
        SemanticType type = resolveType(node.type, node.name);
        TypeSymbol symbol = model.typeSymbolOf(node);
        if (symbol != null) {
            symbol.setType(type);
        }
    }

    @Override
    protected void visitRoutineDeclaration(RoutineDeclaration node) {
        RoutineSymbol routine = model.routineOf(node);
        List<SemanticType> parameterTypes = new ArrayList<>();
        for (Parameter parameter : node.parameters) {
            SemanticType type = resolveType(parameter.type, null);
            VariableSymbol symbol = model.variableOf(parameter);
            if (symbol != null) {
                symbol.setType(type);
            }
            parameterTypes.add(type);
        }
        SemanticType returnType = node.returnType == null ? VoidType.INSTANCE : resolveType(node.returnType, null);
        if (routine != null && !routine.hasSignature()) {
            routine.setSignature(parameterTypes, returnType);
        }
        if (node.body != null) {
            RoutineSymbol outer = currentRoutine;
            currentRoutine = routine;
            visit(node.body);
            currentRoutine = outer;
        }
    }

    @Override
    protected void visitAssignment(AssignmentStatement node) {
        SemanticType target = typeOf(node.target);
        String readOnly = readOnlyReason;
        SemanticType value = typeOf(node.value);
        if (readOnly != null) {
            diagnostics.report(node.target, "cannot assign to " + readOnly);
            return;
        }
        if (!TypeRules.assignable(target, value, node.value)) {
            diagnostics.report(node.value, "cannot assign '" + value + "' to '" + target + "'");
        }
    }

    @Override
    protected void visitPrint(PrintStatement node) {
        for (Expression argument : node.args) {
            typeOf(argument);
        }
    }

    @Override
    protected void visitIf(IfStatement node) {
        requireCondition(node.condition, "if");
        visit(node.thenBody);
        visit(node.elseBody);
    }

    @Override
    protected void visitWhile(WhileStatement node) {
        requireCondition(node.condition, "while");
        visit(node.body);
    }

    @Override
    protected void visitFor(ForStatement node) {
        SemanticType start = typeOf(node.rangeStart);
        if (node.rangeEnd != null) {
            typeOf(node.rangeEnd);
        }
        SemanticType loopType;
        if (node.rangeEnd != null) {
            loopType = PrimitiveType.INTEGER;
        } else if (start instanceof ArrayType array) {
            loopType = array.elementType();
        } else {
            loopType = ErrorType.INSTANCE;
        }
        VariableSymbol symbol = model.variableOf(node);
        if (symbol != null) {
            symbol.setType(loopType);
        }
        visit(node.body);
    }

    @Override
    protected void visitReturn(ReturnStatement node) {
        if (node.value == null) {
            return;
        }
        SemanticType value = typeOf(node.value);
        if (currentRoutine == null || currentRoutine.returnType() == null) {
            return;
        }
        SemanticType expected = currentRoutine.returnType();
        if (expected.isVoid()) {
            return;
        }
        if (!TypeRules.assignable(expected, value, node.value)) {
            diagnostics.report(node.value, "cannot return '" + value + "' from routine returning '"
                    + expected + "'");
        }
    }

    @Override
    protected void visitRoutineCallStatement(RoutineCallStatement node) {
        checkCall(node, node.name, node.args);
    }

    private SemanticType resolveVariable(VariableDeclaration node) {
        SemanticType declared = node.type == null ? null : resolveType(node.type, null);
        SemanticType initial = node.initializer == null ? null : typeOf(node.initializer);
        SemanticType type;
        if (declared != null) {
            type = declared;
            if (initial != null && !TypeRules.assignable(declared, initial, node.initializer)) {
                diagnostics.report(node.initializer, "cannot initialize '" + declared + "' with '" + initial + "'");
            }
        } else {
            type = initial == null ? ErrorType.INSTANCE : initial;
        }
        VariableSymbol symbol = model.variableOf(node);
        if (symbol != null) {
            symbol.setType(type);
        }
        return type;
    }

    private SemanticType resolveType(TypeNode node, String recordName) {
        SemanticType type;
        if (node instanceof PrimitiveTypeNode primitive) {
            type = PrimitiveType.of(PrimitiveType.Kind.valueOf(primitive.kind.name()));
        } else if (node instanceof StringTypeNode) {
            type = StringType.INSTANCE;
        } else if (node instanceof TypeReferenceNode reference) {
            TypeSymbol symbol = model.typeSymbolOf(reference);
            type = symbol == null || symbol.type() == null ? ErrorType.INSTANCE : symbol.type();
        } else if (node instanceof ArrayTypeNode array) {
            SemanticType element = resolveType(array.elementType, null);
            long size = ArrayType.NO_SIZE;
            if (array.sizeExpr != null) {
                typeOf(array.sizeExpr);
                Long value = ConstantEvaluator.integerValue(array.sizeExpr);
                if (value != null && value > 0) {
                    size = value;
                }
            }
            type = new ArrayType(element, size);
        } else if (node instanceof RecordTypeNode record) {
            RecordType recordType = new RecordType(recordName);
            for (VariableDeclaration member : record.members) {
                recordType.addField(member.name, resolveVariable(member));
            }
            type = recordType;
        } else {
            type = ErrorType.INSTANCE;
        }
        model.setType(node, type);
        return type;
    }

    private void requireCondition(Expression condition, String keyword) {
        SemanticType type = typeOf(condition);
        if (!type.isError() && !TypeRules.conformsToBoolean(type, condition)) {
            diagnostics.report(condition, "condition of '" + keyword + "' must be boolean, found '" + type + "'");
        }
    }

    private SemanticType typeOf(Expression expression) {
        SemanticType type = compute(expression);
        model.setType(expression, type);
        return type;
    }

    private SemanticType compute(Expression expression) {
        if (expression instanceof IntegerLiteral) {
            return PrimitiveType.INTEGER;
        }
        if (expression instanceof RealLiteral) {
            return PrimitiveType.REAL;
        }
        if (expression instanceof BooleanLiteral) {
            return PrimitiveType.BOOLEAN;
        }
        if (expression instanceof CharLiteral) {
            return PrimitiveType.CHAR;
        }
        if (expression instanceof StringLiteral) {
            return StringType.INSTANCE;
        }
        if (expression instanceof ModifiablePrimaryNode primary) {
            return typeOfAccess(primary);
        }
        if (expression instanceof UnaryExpression unary) {
            return typeOfUnary(unary);
        }
        if (expression instanceof BinaryExpression binary) {
            return typeOfBinary(binary);
        }
        if (expression instanceof RoutineCallExpression call) {
            SemanticType result = checkCall(call, call.name, call.args);
            if (result.isVoid()) {
                diagnostics.report(call, "routine '" + call.name + "' does not return a value");
                return ErrorType.INSTANCE;
            }
            return result;
        }
        return ErrorType.INSTANCE;
    }

    private SemanticType typeOfAccess(ModifiablePrimaryNode node) {
        VariableSymbol symbol = model.variableOf(node);
        SemanticType type = symbol == null || symbol.type() == null ? ErrorType.INSTANCE : symbol.type();
        String readOnly = null;
        for (Accessor accessor : node.accessors) {
            readOnly = null;
            if (accessor instanceof IndexAccess index) {
                SemanticType indexType = typeOf(index.index);
                if (type.isError()) {
                    continue;
                }
                if (type instanceof ArrayType array) {
                    if (!indexType.isError() && indexType != PrimitiveType.INTEGER) {
                        diagnostics.report(index.index, "array index must be integer, found '" + indexType + "'");
                    }
                    type = array.elementType();
                } else if (type == StringType.INSTANCE) {
                    if (!indexType.isError() && indexType != PrimitiveType.INTEGER) {
                        diagnostics.report(index.index, "string index must be integer, found '" + indexType + "'");
                    }
                    type = PrimitiveType.CHAR;
                    readOnly = "a string element";
                } else {
                    diagnostics.report(index, "cannot index a value of type '" + type + "'");
                    type = ErrorType.INSTANCE;
                }
            } else if (accessor instanceof FieldAccess field) {
                if (type.isError()) {
                    continue;
                }
                if (type instanceof RecordType record) {
                    SemanticType fieldType = record.fieldType(field.name);
                    if (fieldType == null) {
                        diagnostics.report(field, "type '" + record + "' has no field '" + field.name + "'");
                        type = ErrorType.INSTANCE;
                    } else {
                        type = fieldType;
                    }
                } else if ((type instanceof ArrayType || type == StringType.INSTANCE) && field.name.equals("size")) {
                    type = PrimitiveType.INTEGER;
                    readOnly = "'size'";
                } else {
                    diagnostics.report(field, "cannot access field '" + field.name + "' of type '" + type + "'");
                    type = ErrorType.INSTANCE;
                }
            }
        }
        readOnlyReason = readOnly;
        return type;
    }

    private SemanticType typeOfUnary(UnaryExpression node) {
        SemanticType operand = typeOf(node.operand);
        if (operand.isError()) {
            return ErrorType.INSTANCE;
        }
        boolean valid = node.op == TokenType.NOT
                ? TypeRules.conformsToBoolean(operand, node.operand)
                : operand.isNumeric();
        if (!valid) {
            diagnostics.report(node, "operator '" + Operators.symbol(node.op)
                    + "' cannot be applied to '" + operand + "'");
            return ErrorType.INSTANCE;
        }
        return node.op == TokenType.NOT ? PrimitiveType.BOOLEAN : operand;
    }

    private SemanticType typeOfBinary(BinaryExpression node) {
        SemanticType left = typeOf(node.left);
        SemanticType right = typeOf(node.right);
        if (left.isError() || right.isError()) {
            return ErrorType.INSTANCE;
        }
        SemanticType result = binaryResult(node, left, right);
        if (result == null) {
            diagnostics.report(node, "operator '" + Operators.symbol(node.op)
                    + "' cannot be applied to '" + left + "' and '" + right + "'");
            return ErrorType.INSTANCE;
        }
        return result;
    }

    private SemanticType binaryResult(BinaryExpression node, SemanticType left, SemanticType right) {
        boolean numbers = left.isNumeric() && right.isNumeric();
        boolean integers = left == PrimitiveType.INTEGER && right == PrimitiveType.INTEGER;
        SemanticType arithmetic = integers ? PrimitiveType.INTEGER : PrimitiveType.REAL;
        switch (node.op) {
            case PLUS -> {
                if (numbers) {
                    return arithmetic;
                }
                boolean text = (left == StringType.INSTANCE && (right == StringType.INSTANCE
                        || right == PrimitiveType.CHAR))
                        || (left == PrimitiveType.CHAR && right == StringType.INSTANCE);
                return text ? StringType.INSTANCE : null;
            }
            case MINUS, STAR, SLASH -> {
                return numbers ? arithmetic : null;
            }
            case PERCENT -> {
                return integers ? PrimitiveType.INTEGER : null;
            }
            case LT, LE, GT, GE -> {
                return numbers || (left == PrimitiveType.CHAR && right == PrimitiveType.CHAR)
                        ? PrimitiveType.BOOLEAN : null;
            }
            case EQ, NEQ -> {
                return numbers || (left == right && (left == PrimitiveType.BOOLEAN
                        || left == PrimitiveType.CHAR || left == StringType.INSTANCE))
                        ? PrimitiveType.BOOLEAN : null;
            }
            case AND, OR, XOR -> {
                return TypeRules.conformsToBoolean(left, node.left)
                        && TypeRules.conformsToBoolean(right, node.right)
                        ? PrimitiveType.BOOLEAN : null;
            }
            default -> {
                return null;
            }
        }
    }

    private SemanticType checkCall(Node node, String name, List<Expression> arguments) {
        List<SemanticType> argumentTypes = new ArrayList<>();
        for (Expression argument : arguments) {
            argumentTypes.add(typeOf(argument));
        }
        RoutineSymbol routine = model.routineOf(node);
        if (routine == null || !routine.hasSignature()) {
            return ErrorType.INSTANCE;
        }
        List<SemanticType> parameterTypes = routine.parameterTypes();
        if (arguments.size() != parameterTypes.size()) {
            diagnostics.report(node, "routine '" + name + "' expects " + parameterTypes.size()
                    + " argument(s) but " + arguments.size() + " given");
        } else {
            for (int i = 0; i < arguments.size(); i++) {
                if (!TypeRules.assignable(parameterTypes.get(i), argumentTypes.get(i), arguments.get(i))) {
                    diagnostics.report(arguments.get(i), "argument " + (i + 1) + " of '" + name
                            + "': cannot pass '" + argumentTypes.get(i) + "' as '" + parameterTypes.get(i) + "'");
                }
            }
        }
        return routine.returnType();
    }
}
