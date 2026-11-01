package compiler.semantic.visitor;

import static org.junit.jupiter.api.Assertions.*;

import compiler.ast.Program;
import compiler.ast.expression.Expression;
import compiler.ast.expression.ModifiablePrimaryNode;
import compiler.ast.expression.expression.BinaryExpression;
import compiler.ast.expression.expression.UnaryExpression;
import compiler.ast.expression.literal.IntegerLiteral;
import compiler.ast.expression.literal.RealLiteral;
import compiler.ast.statement.PrintStatement;
import compiler.lexer.TokenType;
import compiler.semantic.SemanticTestSupport;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class SourcePrinterTest {

    private static final String CANONICAL = """
            type Item is record
                var price: integer
                var name: string
            end
            type Grid is array[3] array[3] real
            var counter: integer is 0

            routine isOdd(n: integer): boolean

            routine fill(values: array[] integer, count: integer) is
                for i in 1..count loop
                    values[i] := i * 2
                end
                for j in 1..count reverse loop
                    print j
                end
                for v in values loop
                    print v, v
                end
            end

            routine isOdd(n: integer): boolean is
                if n % 2 = 1 then
                    return true
                else
                    return false
                end
            end
            """;

    static List<Path> examples() throws Exception {
        try (Stream<Path> paths = Files.list(Path.of("prog-examples"))) {
            return paths.filter(path -> path.toString().endsWith(".i")).sorted().toList();
        }
    }

    private static String printed(String source) {
        return SourcePrinter.print(SemanticTestSupport.parse(source));
    }

    private static String expression(String source) {
        return printed("var x is " + source + "\n");
    }

    @Test
    void canonicalProgramIsPrintedExactlyAsWritten() {
        assertEquals(CANONICAL, printed(CANONICAL));
    }

    @Test
    void localTypeDeclarationsAreIndentedWithTheirBlock() {
        String source = """
                routine f() is
                    type P is record
                        var a: integer
                    end
                    var p: P
                end
                """;

        assertEquals(source, printed(source));
    }

    @Test
    void messyInputIsNormalized() {
        String messy = "routine sq(x:integer):integer=>x*x\n"
                + "routine hello() is\n"
                + "   foo\n"
                + "   bar(1,2)\n"
                + "   var   z:real is 1.50\n"
                + "end\n";

        assertEquals("""
                routine sq(x: integer): integer is
                    return x * x
                end

                routine hello() is
                    foo()
                    bar(1, 2)
                    var z: real is 1.5
                end
                """, printed(messy));
    }

    @Test
    void emptyBodiesAndSimpleDeclarationsAreNotSeparatedByBlankLines() {
        String source = "var a is 1\nvar b is 2\ntype T is integer\n";

        assertEquals(source, printed(source));
        assertEquals("routine e() is\nend\n", printed("routine e() is\nend\n"));
    }

    @Test
    void emptyProgramPrintsNothing() {
        assertEquals("", printed(""));
    }

    @Test
    void parenthesesAreKeptOnlyWhenPrecedenceRequiresThem() {
        String[][] cases = {
                {"1 + 2 * 3", "1 + 2 * 3"},
                {"(1 + 2) * 3", "(1 + 2) * 3"},
                {"1 - (2 - 3)", "1 - (2 - 3)"},
                {"(1 - 2) - 3", "1 - 2 - 3"},
                {"1 - 2 - 3", "1 - 2 - 3"},
                {"(1 * 2) + 3", "1 * 2 + 3"},
                {"a / (b * c)", "a / (b * c)"},
                {"a * b / c", "a * b / c"},
                {"(a * b) / c", "a * b / c"},
                {"(a and b) or c", "a and b or c"},
                {"a and (b or c)", "a and (b or c)"},
                {"a or (b and c)", "a or (b and c)"},
                {"(a < b) = (c < d)", "(a < b) = (c < d)"},
                {"a + b < c * d", "a + b < c * d"},
                {"(a < b) and (c < d)", "a < b and c < d"},
                {"-5 + 3", "-5 + 3"},
                {"2 * -3", "2 * -3"},
                {"1 - -2", "1 - -2"},
                {"not 1 and true", "not 1 and true"},
                {"(((1)))", "1"},
                {"f(1 + 2, (3))", "f(1 + 2, 3)"},
                {"a[1 + 1].f[(2)]", "a[1 + 1].f[2]"},
        };
        for (String[] pair : cases) {
            assertEquals("var x is " + pair[1] + "\n", expression(pair[0]), pair[0]);
        }
    }

    @Test
    void printedExpressionsParseBackToTheSameTree() {
        String[] sources = {"1 - (2 - 3)", "(a < b) = (c < d)", "a or (b and c)", "a / (b * c)",
                "(1 + 2) * (3 - 4) / 5 % 6", "not 1 and (a or b)", "f(g(1), 2 * (3 + 4))"};
        for (String source : sources) {
            String once = expression(source);
            assertEquals(SemanticTestSupport.parse("var x is " + source + "\n").describe(),
                    SemanticTestSupport.parse(once).describe(), source);
        }
    }

    @Test
    void realLiteralsAlwaysKeepADecimalPoint() {
        assertEquals("var x is 1.5\n", expression("1.5"));
        assertEquals("var x is 0.25\n", expression("0.25"));
        assertEquals("var x is 100.0\n", expression("100.0"));
        assertEquals("var x is 10000000.0\n", expression("10000000.0"));
        String tiny = expression("0.000001");
        assertEquals(SemanticTestSupport.parse("var x is 0.000001\n").describe(),
                SemanticTestSupport.parse(tiny).describe());
    }

    @Test
    void charAndStringLiteralsAreEscapedLikeTheLexerExpects() {
        String[] literals = {"'a'", "'\\n'", "'\\t'", "'\\''", "'\\\\'", "'\"'",
                "\"say \\\"hi\\\"\\n\\t\\\\\"", "\"it's\"", "\"\"", "'я'", "\"привет\""};
        for (String literal : literals) {
            assertEquals("var x is " + literal + "\n", expression(literal), literal);
        }
    }

    @Test
    void negativeNumbersBuiltByTransformationsAreWrittenWithASign() {
        Expression minusThree = new IntegerLiteral(1, 1, -3);
        Expression variable = new ModifiablePrimaryNode(1, 1, "a", List.of());
        Expression difference = new BinaryExpression(1, 1, TokenType.MINUS, variable, minusThree);

        assertEquals("-3", SourcePrinter.print(minusThree));
        assertEquals("-1.5", SourcePrinter.print(new RealLiteral(1, 1, -1.5)));
        assertEquals("a - -3", SourcePrinter.print(difference));
    }

    @Test
    void unaryOperandsThatAreNotPrimaryAreParenthesized() {
        Expression sum = SemanticTestSupport.expression("1 + 2");

        assertEquals("-(1 + 2)", SourcePrinter.print(new UnaryExpression(1, 1, TokenType.MINUS, sum)));
        assertEquals("not (1 + 2)", SourcePrinter.print(new UnaryExpression(1, 1, TokenType.NOT, sum)));
    }

    @Test
    void anyNodeCanBePrintedOnItsOwn() {
        assertEquals("1 + 2", SourcePrinter.print(SemanticTestSupport.expression("1 + 2")));
        PrintStatement print = new PrintStatement(1, 1, List.of(SemanticTestSupport.expression("1")));
        assertEquals("print 1\n", SourcePrinter.print(print));
    }

    @Test
    void printingIsIdempotent() {
        String once = printed(CANONICAL);

        assertEquals(once, printed(once));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("examples")
    void printedExamplesParseBackToTheSameTree(Path source) throws Exception {
        Program original = SemanticTestSupport.parse(Files.readString(source));
        String text = SourcePrinter.print(original);
        Program again = SemanticTestSupport.parse(text);

        assertEquals(original.describe(), again.describe());
        assertEquals(text, SourcePrinter.print(again));
    }
}
