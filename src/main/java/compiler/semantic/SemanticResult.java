package compiler.semantic;

import java.util.List;

public record SemanticResult(SemanticModel model, List<Diagnostic> diagnostics) {

    public boolean hasErrors() {
        return !diagnostics.isEmpty();
    }
}
