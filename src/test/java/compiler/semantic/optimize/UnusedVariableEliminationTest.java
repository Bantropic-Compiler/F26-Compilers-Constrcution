package compiler.semantic.optimize;

import compiler.semantic.SemanticAnalyzer;
import compiler.semantic.SemanticTestSupport;
import compiler.semantic.visitor.SourcePrinter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UnusedVariableEliminationTest {
    private String fixture(String name) throws IOException {
        String path = "/semantic/optimize/unused-variable/" + name + ".i";
        try (var stream = getClass().getResourceAsStream(path)) {
            assertNotNull(stream, path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void check(String input, String expected, Optimizer optimizer, boolean changed) throws IOException {
        var program = SemanticTestSupport.parse(fixture(input));
        var analysis = new SemanticAnalyzer().analyze(program);
        assertFalse(analysis.hasErrors(), () -> analysis.diagnostics().toString());
        var result = optimizer.optimize(program, analysis);
        assertFalse(result.hasErrors(), () -> result.semanticResult().diagnostics().toString());
        assertEquals(changed, result.changed());
        String printed = SourcePrinter.print(program);
        assertEquals(SourcePrinter.print(SemanticTestSupport.parse(fixture(expected))), printed);
        assertFalse(SemanticTestSupport.analyze(printed).hasErrors());
        assertFalse(optimizer.optimize(program, result.semanticResult()).changed());
    }

    @Test void removesUnreadLocalsAndWritesAcrossRounds() throws IOException {
        check("remove", "remove.expected", new Optimizer(List.of(new UnusedVariableElimination())), true);
    }

    @Test void preservesEffectsErrorsAndReads() throws IOException {
        check("keep", "keep", new Optimizer(List.of(new UnusedVariableElimination())), false);
    }

    @Test void distinguishesShadowedSymbols() throws IOException {
        check("shadow", "shadow.expected", new Optimizer(List.of(new UnusedVariableElimination())), true);
    }

    @Test void worksWithPropagationAndFolding() throws IOException {
        check("combined", "combined.expected", new Optimizer(), true);
    }
}
