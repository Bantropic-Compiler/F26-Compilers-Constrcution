package compiler.semantic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import compiler.TestPrograms;
import compiler.ast.Program;

class SemanticAnalyzerTest {

    static Stream<Path> examples() throws IOException {
        try (Stream<Path> files = Files.list(Path.of("prog-examples"))) {
            return files.filter(path -> path.toString().endsWith(".i")).sorted().toList().stream();
        }
    }

    @ParameterizedTest
    @MethodSource("examples")
    void exampleProgramsHaveNoDiagnostics(Path example) throws IOException {
        Program program = SemanticTestSupport.parse(Files.readString(example));
        SemanticResult result = new SemanticAnalyzer().analyze(program);

        assertEquals(List.of(), result.diagnostics());
        assertFalse(result.hasErrors());
    }

    @Test
    void undeclaredNamesAndTypeMismatchesAreAllReported() {
        assertEquals(List.of(
                "1:23: cannot initialize 'integer' with 'string'",
                "2:22: cannot initialize 'boolean' with 'integer'",
                "5:16: 'hight' is not declared",
                "9:14: routine 'area' expects 2 argument(s) but 1 given",
                "10:8: condition of 'if' must be boolean, found 'integer'",
                "13:5: 'undefined' is not declared"),
                SemanticTestSupport.diagnostics(TestPrograms.read("semantic-undeclared-and-types.i")));
    }

    @Test
    void forwardDeclarationProblemsAreReported() {
        assertEquals(List.of(
                "2:1: routine 'never' is declared but never defined",
                "4:1: routine 'twice' does not match its forward declaration at 1:1"),
                SemanticTestSupport.diagnostics(TestPrograms.read("semantic-forward-mismatch.i")));
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
