package compiler.lexer;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;

class GoldenFileTest {
    static List<Path> examples() throws Exception {
        try (Stream<Path> paths = Files.list(Path.of("prog-examples"))) {
            return paths.filter(path -> path.toString().endsWith(".i")).sorted().toList();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("examples")
    void matchesReviewedTokenFixture(Path source) throws Exception {
        var result = CategoryTestDriver.scan(Files.readString(source));
        assertTrue(result.errors().isEmpty(), () -> result.errors().toString());
        String fixture = "/lexer/" + source.getFileName().toString().replace(".i", ".tokens");
        try (var stream = getClass().getResourceAsStream(fixture)) {
            assertNotNull(stream, "Missing golden fixture: " + fixture);
            String expected = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            StringBuilder actual = new StringBuilder();
            for (Token token : result.tokens()) {
                Object value = token.value();
                String typedValue = value == null ? "-" : value.getClass().getSimpleName() + ":" + value;
                actual.append(token.line()).append(':').append(token.column()).append('\t')
                        .append(token.type()).append('\t').append(escape(token.lexeme())).append('\t')
                        .append(escape(typedValue)).append('\n');
            }
            assertEquals(expected, actual.toString());
        }
    }

    private static String escape(String text) {
        return text.replace("\\", "\\\\").replace("\t", "\\t")
                .replace("\r", "\\r").replace("\n", "\\n");
    }
}
