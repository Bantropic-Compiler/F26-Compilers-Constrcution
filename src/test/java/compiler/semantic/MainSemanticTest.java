package compiler.semantic;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MainSemanticTest {

    @TempDir
    Path directory;

    private Path write(String text) throws IOException {
        return Files.writeString(directory.resolve("program.i"), text);
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

    @Test
    void validProgramIsPrintedAsSourceText() throws Exception {
        Result result = launch(write("var x is 1+2\n"));

        assertEquals(0, result.status());
        assertEquals("var x is 1 + 2\n", result.output());
        assertEquals("", result.errors());
    }

    @Test
    void noOptFlagStillPrintsSource() throws Exception {
        Result result = launch(write("var x is 1\n"), "--no-opt");

        assertEquals(0, result.status());
        assertEquals("var x is 1\n", result.output());
    }

    @Test
    void semanticErrorsAreSortedPrintedToStderrWithoutOutput() throws Exception {
        Result result = launch(write("var a: boolean is 2\nvar b is missing\n"));

        assertEquals(1, result.status());
        assertEquals("", result.output());
        assertEquals("1:19: cannot initialize 'boolean' with 'integer'\n2:10: 'missing' is not declared\n",
                result.errors());
    }

    @Test
    void syntaxErrorsStopBeforeSemanticAnalysis() throws Exception {
        Result result = launch(write("var a is\nvar b is missing\n"));

        assertEquals(1, result.status());
        assertEquals("", result.output());
        assertTrue(!result.errors().contains("not declared"));
    }

    @Test
    void usageErrorsExitWithTwo() throws Exception {
        Result unknownFlag = launch(write("var x is 1\n"), "--bogus");
        assertEquals(2, unknownFlag.status());
        assertTrue(unknownFlag.errors().startsWith("usage:"));
    }

    @Test
    void missingFileIsReported() throws Exception {
        Result result = launch(directory.resolve("absent.i"));

        assertEquals(1, result.status());
        assertEquals("", result.output());
    }

    private record Result(int status, String output, String errors) {
    }
}
