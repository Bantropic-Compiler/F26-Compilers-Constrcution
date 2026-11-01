package compiler.semantic.check;

import compiler.ast.Program;
import compiler.ast.declaration.Parameter;
import compiler.ast.declaration.RoutineDeclaration;
import compiler.semantic.Diagnostics;
import compiler.semantic.SemanticModel;
import compiler.semantic.symbol.RoutineSymbol;
import compiler.semantic.symbol.Symbol;
import compiler.semantic.type.ErrorType;
import compiler.semantic.type.SemanticType;
import compiler.semantic.type.VoidType;

public final class SignatureChecker implements Check {

    @Override
    public void run(Program program, SemanticModel model, Diagnostics diagnostics) {
        for (Symbol symbol : model.globalScope().symbols()) {
            if (!(symbol instanceof RoutineSymbol routine)) {
                continue;
            }
            RoutineDeclaration forward = routine.forwardDeclaration();
            RoutineDeclaration definition = routine.definition();
            if (forward == null || definition == null || forward == definition) {
                continue;
            }
            if (!sameSignature(model, forward, definition)) {
                diagnostics.report(definition, "routine '" + routine.name()
                        + "' does not match its forward declaration at " + forward.line + ":" + forward.column);
            }
        }
    }

    private static boolean sameSignature(SemanticModel model, RoutineDeclaration forward,
                                         RoutineDeclaration definition) {
        if (forward.parameters.size() != definition.parameters.size()) {
            return false;
        }
        for (int i = 0; i < forward.parameters.size(); i++) {
            Parameter left = forward.parameters.get(i);
            Parameter right = definition.parameters.get(i);
            if (!same(model.typeOf(left.type), model.typeOf(right.type))) {
                return false;
            }
        }
        return same(returnType(model, forward), returnType(model, definition));
    }

    private static SemanticType returnType(SemanticModel model, RoutineDeclaration declaration) {
        return declaration.returnType == null ? VoidType.INSTANCE : model.typeOf(declaration.returnType);
    }

    private static boolean same(SemanticType left, SemanticType right) {
        return left instanceof ErrorType || right instanceof ErrorType || left.equals(right);
    }
}
