package compiler.semantic.optimize;

import compiler.semantic.*;
import compiler.semantic.visitor.SourcePrinter;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class ConstantPropagationTest {
    private String fixture(String name) {
        String path = "/semantic/optimize/constant-propagation/" + name;
        try (var stream = getClass().getResourceAsStream(path)) {
            assertNotNull(stream, "Missing fixture: " + path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read fixture: " + path, e);
        }
    }

    private String optimize(String source) {
        var program = SemanticTestSupport.parse(source);
        var analysis = new SemanticAnalyzer().analyze(program);
        assertFalse(analysis.hasErrors(), () -> analysis.diagnostics().toString());
        var result = new Optimizer().optimize(program, analysis);
        assertFalse(result.hasErrors(), () -> result.semanticResult().diagnostics().toString());
        assertFalse(new Optimizer().optimize(program, result.semanticResult()).changed());
        String printed = SourcePrinter.print(program);
        assertFalse(SemanticTestSupport.analyze(printed).hasErrors());
        return printed;
    }

    @Test void propagatesAcrossRoundsAndFolds() {
        String result = optimize(fixture("across-rounds.i"));
        assertFalse(result.contains("var a"));
        assertFalse(result.contains("var b"));
        assertTrue(result.contains("print 6"));
    }

    @Test void anyAssignmentPreventsPropagation() {
        String result = optimize(fixture("local-assignment.i"));
        assertTrue(result.contains("print a"));
        assertTrue(result.contains("a := 4"));
    }

    @Test void writesInRoutinesPreventGlobalPropagation() {
        String result = optimize(fixture("global-assignment.i"));
        assertTrue(result.contains("print a"));
    }

    @Test void symbolsDistinguishShadowing() {
        String result = optimize(fixture("shadowing.i"));
        assertTrue(result.contains("print 7"));
        assertTrue(result.contains("print 3"));
    }

    @Test void preservesAssignmentConversions() {
        String result = optimize(fixture("assignment-conversions.i"));
        assertTrue(result.contains("print 1.5"));
        assertTrue(result.contains("print 3"));
        assertTrue(result.contains("print true"));
        assertTrue(result.contains("print 1"));
        assertTrue(result.contains("print 'a'"));
    }

    @Test void leavesAmbiguousRoundingAndStrings() {
        String result = optimize(fixture("rounding-and-strings.i"));
        assertTrue(result.contains("print i, s"));
    }

    @Test void reportsErrorsExposedByPropagation() {
        var program = SemanticTestSupport.parse(fixture("division-by-zero.i"));
        var analysis = new SemanticAnalyzer().analyze(program);
        assertFalse(analysis.hasErrors());
        var result = new Optimizer().optimize(program, analysis);
        assertTrue(result.hasErrors());
        assertEquals(ConstantPropagation.class.getName(), result.failedPass());
    }
}
