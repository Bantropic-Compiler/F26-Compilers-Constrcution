package compiler;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {

    @Test
    void printsAstForWholeSourceFile() throws Exception {
        Path source = Path.of("prog-examples/01-print-number.i");
        Result result = launch(source);

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

    private static Result launch(Path source) throws Exception {
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        String classes = Path.of("target/classes").toAbsolutePath().toString();
        Process process = new ProcessBuilder(java, "-cp", classes, "compiler.Main",
                source.toAbsolutePath().toString()).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String errors = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        return new Result(process.waitFor(), output, errors);
    }

    private record Result(int status, String output, String errors) {}
}
