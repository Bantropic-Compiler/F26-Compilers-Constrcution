package compiler;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {

    @Test
    void printsAstForWholeSourceFile() throws Exception {
        Path source = Path.of("prog-examples/01-print-number.i");
        Result result = launch(source, "--ast");

        assertEquals(0, result.status());
        assertEquals(Files.readString(Path.of("src/test/resources/parser/01-print-number.ast")),
                result.output());
        assertEquals("", result.errors());
    }

    @Test
    void printsEveryRecoveredSyntaxErrorWithoutPartialTree() throws Exception {
        Path source = Path.of("prog-examples/test-cases/recovery-cli.i");
        Result result = launch(source);

        assertEquals(1, result.status());
        assertEquals("", result.output());
        String diagnostics = result.errors();
        assertEquals(2, diagnostics.lines().count());
        assertTrue(diagnostics.contains("1:"));
        assertTrue(diagnostics.contains("2:"));
    }

    @Test
    void runsPipelineWithAllFlagCombinations() throws Exception {
        Path source = Path.of("src/test/resources/semantic/optimize/unused-variable/combined.i");
        var original = compiler.semantic.SemanticTestSupport.parse(Files.readString(source));
        var expected = compiler.semantic.SemanticTestSupport.parse(Files.readString(
                source.resolveSibling("combined.expected.i")));
        for (boolean noOpt : new boolean[]{false, true}) {
            for (boolean ast : new boolean[]{false, true}) {
                List<String> flags = new ArrayList<>();
                if (noOpt) flags.add("--no-opt");
                if (ast) flags.add("--ast");
                Result result = launch(source, flags.toArray(String[]::new));
                var tree = noOpt ? original : expected;
                assertEquals(0, result.status());
                assertEquals("", result.errors());
                assertEquals(ast ? tree.describe() : compiler.semantic.visitor.SourcePrinter.print(tree), result.output());
            }
        }
    }

    @Test
    void reportsOptimizationFailureWithoutPrintingPartialResult() throws Exception {
        Path source = Path.of("src/test/resources/semantic/optimize/constant-propagation/division-by-zero.i");
        Result result = launch(source);
        assertEquals(1, result.status());
        assertEquals("", result.output());
        assertTrue(result.errors().contains("ConstantPropagation"));
        assertEquals(0, launch(source, "--no-opt").status());
    }

    @Test
    void rejectsUnknownFlag() throws Exception {
        Result result = launch(Path.of("prog-examples/01-print-number.i"), "--unknown");
        assertEquals(2, result.status());
        assertEquals("", result.output());
        assertTrue(result.errors().contains("usage:"));
    }

    private static Result launch(Path source, String... flags) throws Exception {
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        String classes = Path.of("target/classes").toAbsolutePath().toString();
        List<String> command = new ArrayList<>(List.of(java, "-cp", classes, "compiler.Main"));
        command.addAll(List.of(flags));
        command.add(source.toAbsolutePath().toString());
        Process process = new ProcessBuilder(command).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String errors = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        return new Result(process.waitFor(), output, errors);
    }

    private record Result(int status, String output, String errors) {}
}
