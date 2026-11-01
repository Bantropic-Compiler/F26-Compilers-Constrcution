package compiler.semantic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import compiler.TestPrograms;
import compiler.ast.Program;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class SemanticAnalyzerTest {

    static Stream<Path> examples() throws IOException {
        try (Stream<Path> files = Files.list(Path.of("prog-examples"))) {
            return files.filter(path -> path.toString().endsWith(".i")).sorted().toList().stream();
        }
    }

    static Stream<String> negativePrograms() {
        return Stream.of("semantic-undeclared-and-types", "semantic-forward-mismatch");
    }

    @ParameterizedTest
    @MethodSource("examples")
    void exampleProgramsHaveNoDiagnostics(Path example) throws IOException {
        Program program = SemanticTestSupport.parse(Files.readString(example));
        SemanticResult result = new SemanticAnalyzer().analyze(program);

        assertEquals(List.of(), result.diagnostics());
        assertFalse(result.hasErrors());
    }

    @ParameterizedTest
    @MethodSource("negativePrograms")
    void negativeProgramsReportExactlyTheExpectedDiagnostics(String name) throws IOException {
        SemanticResult result = SemanticTestSupport.analyze(TestPrograms.read(name + ".i"));
        List<String> expected = Files.readAllLines(Path.of("src/test/resources/semantic", name + ".diagnostics"));

        assertTrue(result.hasErrors());
        assertEquals(expected, result.diagnostics().stream().map(Diagnostic::toString).toList());
    }

    @Test
    void validFeatureProgramHasNoDiagnostics() {
        assertEquals(List.of(), SemanticTestSupport.diagnostics(TestPrograms.read("semantic-valid-all-features.i")));
    }

    @Test
    void diagnosticsFromAllPassesAreMergedAndSortedByPosition() {
        assertEquals(List.of(
                "2:10: 'missing' is not declared",
                "3:1: routine 'f' does not match its forward declaration at 1:1"), SemanticTestSupport.diagnostics("""
                routine f(a: integer)
                var x is missing
                routine f(a: real) is
                end
                """));
    }

    @Test
    void allPassesRunEvenAfterAnEarlierPassFails() {
        List<String> messages = SemanticTestSupport.diagnostics("""
                var a is missing
                var b: boolean is 2
                """);

        assertEquals(2, messages.size());
    }

    @Test
    void identicalDiagnosticsAreReportedOnce() {
        List<String> messages = SemanticTestSupport.diagnostics("var a is missing\n");

        assertEquals(1, messages.size());
    }

    @Test
    void resultExposesTheModel() {
        SemanticResult result = SemanticTestSupport.analyze("var a is 1\n");

        assertFalse(result.hasErrors());
        assertTrue(result.model().globalScope().lookupLocal("a") != null);
    }
}
