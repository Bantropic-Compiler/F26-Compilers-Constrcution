package compiler.semantic;

import compiler.ast.Program;
import compiler.semantic.check.Check;
import compiler.semantic.check.ContextChecker;
import compiler.semantic.check.NameResolver;
import compiler.semantic.check.LoopChecker;
import compiler.semantic.check.SignatureChecker;
import compiler.semantic.check.TypeChecker;
import java.util.List;

public final class SemanticAnalyzer {

    public SemanticResult analyze(Program program) {
        SemanticModel model = new SemanticModel();
        Diagnostics diagnostics = new Diagnostics();
        for (Check check : checks()) {
            check.run(program, model, diagnostics);
        }
        return new SemanticResult(model, diagnostics.sorted());
    }

    private static List<Check> checks() {
        return List.of(
                    new NameResolver(), 
                    new TypeChecker(), 
                    new SignatureChecker(), 
                    new ContextChecker(),
                    new LoopChecker()
                );
    }
}
