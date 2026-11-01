package compiler.semantic;

import static org.junit.jupiter.api.Assertions.assertTrue;

import compiler.ast.Program;
import compiler.ast.declaration.VariableDeclaration;
import compiler.ast.expression.Expression;
import compiler.lexer.Lexer;
import compiler.parser.Parser;
import java.util.List;

public final class SemanticTestSupport {

    private SemanticTestSupport() {
    }

    public static Program parse(String source) {
        Parser parser = new Parser(new Lexer(source));
        Program program = Program.parse(parser);
        assertTrue(parser.getErrors().isEmpty(), () -> "syntax errors: " + parser.getErrors());
        return program;
    }

    public static Expression expression(String source) {
        Program program = parse("var x is " + source + "\n");
        return ((VariableDeclaration) program.declarations.get(0)).initializer;
    }

    public static SemanticResult analyze(String source) {
        return new SemanticAnalyzer().analyze(parse(source));
    }

    public static List<String> diagnostics(String source) {
        return analyze(source).diagnostics().stream().map(Diagnostic::toString).toList();
    }

    public static String inRoutine(String... lines) {
        StringBuilder text = new StringBuilder("routine t() is\n");
        for (String line : lines) {
            text.append("    ").append(line).append('\n');
        }
        return text.append("end\n").toString();
    }
}
