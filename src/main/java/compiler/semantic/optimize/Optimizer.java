package compiler.semantic.optimize;

import compiler.ast.Program;
import compiler.semantic.SemanticAnalyzer;
import compiler.semantic.SemanticResult;
import java.util.List;

public final class Optimizer {

    private final List<Optimization> passes;

    public Optimizer() {
        this(optimizations());
    }

    Optimizer(List<Optimization> passes) {
        this.passes = List.copyOf(passes);
    }

    private static List<Optimization> optimizations() {
        return List.of(new ConstantPropagation(), new ConstantFolding());
    }

    /**
     * Runs passes until a complete round leaves the AST unchanged.
     * The supplied analysis must describe this exact tree in its current state.
     */
    public OptimizationResult optimize(Program program, SemanticResult analysis) {
        SemanticAnalyzer analyzer = new SemanticAnalyzer();
        if (analysis.hasErrors()) {
            return new OptimizationResult(program, analysis, false, null);
        }

        boolean changed = false;
        boolean roundChanged;
        do {
            roundChanged = false;
            for (Optimization pass : passes) {
                if (pass.run(program, analysis.model())) {
                    changed = true;
                    roundChanged = true;
                    analysis = analyzer.analyze(program);
                    if (analysis.hasErrors()) {
                        return new OptimizationResult(program, analysis, true, pass.getClass().getName());
                    }
                }
            }
        } while (roundChanged);
        return new OptimizationResult(program, analysis, changed, null);
    }
}
