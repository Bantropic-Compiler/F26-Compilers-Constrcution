package compiler.semantic.check;

import compiler.ast.BlockNode;
import compiler.ast.Node;
import compiler.ast.Program;
import compiler.ast.declaration.Parameter;
import compiler.ast.declaration.RoutineDeclaration;
import compiler.ast.declaration.TypeDeclaration;
import compiler.ast.declaration.VariableDeclaration;
import compiler.ast.expression.ModifiablePrimaryNode;
import compiler.ast.expression.expression.RoutineCallExpression;
import compiler.ast.statement.loop.ForStatement;
import compiler.ast.statement.routine.RoutineCallStatement;
import compiler.ast.type.RecordTypeNode;
import compiler.ast.type.TypeReferenceNode;
import compiler.semantic.Diagnostics;
import compiler.semantic.SemanticModel;
import compiler.semantic.symbol.RoutineSymbol;
import compiler.semantic.symbol.Scope;
import compiler.semantic.symbol.Symbol;
import compiler.semantic.symbol.TypeSymbol;
import compiler.semantic.symbol.VariableSymbol;
import compiler.semantic.visitor.AstVisitor;
import java.util.HashSet;
import java.util.Set;

public final class NameResolver extends AstVisitor implements Check {

    private SemanticModel model;
    private Diagnostics diagnostics;
    private Scope scope;

    @Override
    public void run(Program program, SemanticModel model, Diagnostics diagnostics) {
        this.model = model;
        this.diagnostics = diagnostics;
        this.scope = model.globalScope();
        model.setScope(program, scope);
        visit(program);
    }

    @Override
    protected void visitProgram(Program node) {
        super.visitProgram(node);
        for (Symbol symbol : model.globalScope().symbols()) {
            if (symbol instanceof RoutineSymbol routine && !routine.isDefined()) {
                diagnostics.report(routine.declaration(),
                        "routine '" + routine.name() + "' is declared but never defined");
            }
        }
    }

    @Override
    protected void visitBlock(BlockNode node) {
        Scope outer = scope;
        scope = new Scope(outer);
        model.setScope(node, scope);
        super.visitBlock(node);
        scope = outer;
    }

    @Override
    protected void visitVariableDeclaration(VariableDeclaration node) {
        visit(node.type);
        visit(node.initializer);
        VariableSymbol.Kind kind = scope.isGlobal() ? VariableSymbol.Kind.GLOBAL : VariableSymbol.Kind.LOCAL;
        VariableSymbol symbol = new VariableSymbol(node.name, node, kind);
        model.bind(node, symbol);
        declare(symbol, node);
    }

    @Override
    protected void visitTypeDeclaration(TypeDeclaration node) {
        visit(node.type);
        TypeSymbol symbol = new TypeSymbol(node.name, node);
        model.bind(node, symbol);
        declare(symbol, node);
    }

    @Override
    protected void visitRoutineDeclaration(RoutineDeclaration node) {
        Scope outer = scope;
        Scope parameters = new Scope(outer);
        model.setScope(node, parameters);
        for (Parameter parameter : node.parameters) {
            visit(parameter.type);
            VariableSymbol symbol = new VariableSymbol(parameter.name, parameter, VariableSymbol.Kind.PARAMETER);
            model.bind(parameter, symbol);
            if (parameters.lookupLocal(parameter.name) != null) {
                diagnostics.report(parameter, "parameter '" + parameter.name + "' is already declared");
            } else {
                parameters.declare(symbol);
            }
        }
        visit(node.returnType);
        declareRoutine(node);
        scope = parameters;
        visit(node.body);
        scope = outer;
    }

    @Override
    protected void visitFor(ForStatement node) {
        visit(node.rangeStart);
        visit(node.rangeEnd);
        Scope outer = scope;
        Scope loop = new Scope(outer);
        model.setScope(node, loop);
        VariableSymbol symbol = new VariableSymbol(node.loopVar, node, VariableSymbol.Kind.LOOP);
        model.bind(node, symbol);
        loop.declare(symbol);
        scope = loop;
        visit(node.body);
        scope = outer;
    }

    @Override
    protected void visitModifiablePrimary(ModifiablePrimaryNode node) {
        Symbol symbol = scope.lookup(node.base);
        if (symbol == null) {
            diagnostics.report(node, "'" + node.base + "' is not declared");
        } else if (!(symbol instanceof VariableSymbol)) {
            diagnostics.report(node, "'" + node.base + "' is not a variable");
        } else {
            model.bind(node, symbol);
        }
        super.visitModifiablePrimary(node);
    }

    @Override
    protected void visitRoutineCallExpression(RoutineCallExpression node) {
        resolveRoutine(node, node.name);
        super.visitRoutineCallExpression(node);
    }

    @Override
    protected void visitRoutineCallStatement(RoutineCallStatement node) {
        resolveRoutine(node, node.name);
        super.visitRoutineCallStatement(node);
    }

    @Override
    protected void visitTypeReference(TypeReferenceNode node) {
        Symbol symbol = scope.lookup(node.name);
        if (symbol == null) {
            diagnostics.report(node, "'" + node.name + "' is not declared");
        } else if (!(symbol instanceof TypeSymbol)) {
            diagnostics.report(node, "'" + node.name + "' is not a type");
        } else {
            model.bind(node, symbol);
        }
    }

    @Override
    protected void visitRecordType(RecordTypeNode node) {
        Set<String> names = new HashSet<>();
        for (VariableDeclaration member : node.members) {
            visit(member.type);
            visit(member.initializer);
            model.bind(member, new VariableSymbol(member.name, member, VariableSymbol.Kind.FIELD));
            if (!names.add(member.name)) {
                diagnostics.report(member, "field '" + member.name + "' is already declared in this record");
            }
        }
    }

    private void declare(Symbol symbol, Node at) {
        if (scope.lookupLocal(symbol.name()) != null) {
            diagnostics.report(at, "'" + symbol.name() + "' is already declared in this scope");
            return;
        }
        scope.declare(symbol);
    }

    private void declareRoutine(RoutineDeclaration node) {
        Symbol existing = scope.lookupLocal(node.name);
        RoutineSymbol routine;
        if (existing == null) {
            routine = new RoutineSymbol(node.name, node);
            scope.declare(routine);
        } else if (existing instanceof RoutineSymbol known) {
            routine = known;
            if (node.body == null) {
                diagnostics.report(node, "routine '" + node.name + "' is already declared");
            } else if (known.isDefined()) {
                diagnostics.report(node, "routine '" + node.name + "' is already defined");
            } else {
                known.setDefinition(node);
            }
        } else {
            diagnostics.report(node, "'" + node.name + "' is already declared in this scope");
            routine = new RoutineSymbol(node.name, node);
        }
        model.bind(node, routine);
    }

    private void resolveRoutine(Node node, String name) {
        Symbol symbol = scope.lookup(name);
        if (symbol == null) {
            diagnostics.report(node, "'" + name + "' is not declared");
        } else if (!(symbol instanceof RoutineSymbol)) {
            diagnostics.report(node, "'" + name + "' is not a routine");
        } else {
            model.bind(node, symbol);
        }
    }
}
