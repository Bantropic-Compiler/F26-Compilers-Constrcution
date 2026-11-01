package compiler.semantic.symbol;

import compiler.ast.declaration.RoutineDeclaration;
import compiler.semantic.type.SemanticType;
import java.util.List;

public final class RoutineSymbol extends Symbol {

    private RoutineDeclaration forwardDeclaration;
    private RoutineDeclaration definition;
    private List<SemanticType> parameterTypes;
    private SemanticType returnType;

    public RoutineSymbol(String name, RoutineDeclaration first) {
        super(name, first);
        if (first.body == null) {
            this.forwardDeclaration = first;
        } else {
            this.definition = first;
        }
    }

    public RoutineDeclaration forwardDeclaration() {
        return forwardDeclaration;
    }

    public RoutineDeclaration definition() {
        return definition;
    }

    public void setDefinition(RoutineDeclaration definition) {
        this.definition = definition;
    }

    public boolean isDefined() {
        return definition != null;
    }

    public boolean hasSignature() {
        return parameterTypes != null;
    }

    public void setSignature(List<SemanticType> parameterTypes, SemanticType returnType) {
        this.parameterTypes = List.copyOf(parameterTypes);
        this.returnType = returnType;
    }

    public List<SemanticType> parameterTypes() {
        return parameterTypes;
    }

    public SemanticType returnType() {
        return returnType;
    }
}
