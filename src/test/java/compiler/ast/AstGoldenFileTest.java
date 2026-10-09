package compiler.ast;

import compiler.lexer.Lexer;
import compiler.lexer.TokenType;
import compiler.parser.Parser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

class AstGoldenFileTest {

    static List<Path> examples() throws Exception {
        try (Stream<Path> paths = Files.list(Path.of("prog-examples"))) {
            return paths.filter(path -> path.toString().endsWith(".i")).sorted().toList();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("examples")
    void matchesAstFixture(Path source) throws Exception {
        Parser parser = new Parser(new Lexer(Files.readString(source)));
        Program program = Program.parse(parser);

        assertTrue(parser.getErrors().isEmpty(), () -> source + ": " + parser.getErrors());
        assertEquals(TokenType.EOF, parser.getCurrent().type());

        String fixture = "/parser/" + source.getFileName().toString().replace(".i", ".ast");
        try (var stream = getClass().getResourceAsStream(fixture)) {
            assertNotNull(stream, "Missing golden fixture: " + fixture);
            String expected = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(expected, program.describe(), "AST differs for " + source);
        }
    }
}
