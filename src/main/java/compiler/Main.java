package compiler;

import compiler.ast.Program;
import compiler.lexer.Lexer;
import compiler.lexer.LexerException;
import compiler.parser.Parser;
import compiler.parser.ParserException;
import compiler.semantic.Diagnostic;
import compiler.semantic.SemanticAnalyzer;
import compiler.semantic.SemanticResult;
import compiler.semantic.visitor.SourcePrinter;
import compiler.semantic.optimize.Optimizer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Parses, checks and optimizes I source; prints source or AST (no code generation yet).
 * Run from the repository root:
 * mvn compile
 * java -cp target/classes compiler.Main prog-examples/01-print-number.i
 * java -cp target/classes compiler.Main --no-opt prog-examples/01-print-number.i
 * java -cp target/classes compiler.Main --ast prog-examples/01-print-number.i
 * java -cp target/classes compiler.Main --no-opt --ast prog-examples/01-print-number.i
 * Exit codes: 0 success, 1 compilation/read error, 2 invalid arguments.
 */
public class Main {

    public static void main(String[] args) {
        boolean optimize = true;
        boolean printAst = false;
        String path = null;
        for (String arg : args) {
            if (arg.equals("--no-opt")) {
                optimize = false;
            } else if (arg.equals("--ast")) {
                printAst = true;
            } else if (arg.startsWith("--") || path != null) {
                usage();
                return;
            } else {
                path = arg;
            }
        }
        if (path == null) {
            usage();
            return;
        }

        try {
            String source = Files.readString(Path.of(path));
            Parser parser = new Parser(new Lexer(source));
            Program program = Program.parse(parser);

            if (!parser.getErrors().isEmpty()) {
                for (ParserException error : parser.getErrors()) {
                    System.err.println(error.getMessage());
                }
                System.exit(1);
                return;
            }

            SemanticResult analysis = new SemanticAnalyzer().analyze(program);
            if (analysis.hasErrors()) {
                for (Diagnostic diagnostic : analysis.diagnostics()) {
                    System.err.println(diagnostic);
                }
                System.exit(1);
                return;
            }

            if (optimize) {
                var result = new Optimizer().optimize(program, analysis);
                program = result.program();
                analysis = result.semanticResult();
                if (result.hasErrors()) {
                    System.err.println("Optimization failed in " + result.failedPass());
                    for (Diagnostic diagnostic : analysis.diagnostics()) {
                        System.err.println(diagnostic);
                    }
                    System.exit(1);
                    return;
                }
            }

            System.out.print(printAst ? program.describe() : SourcePrinter.print(program));
        } catch (IOException | LexerException | ParserException error) {
            System.err.println(error.getMessage());
            System.exit(1);
        }
    }

    private static void usage() {
        System.err.println("usage: Main [--no-opt] [--ast] <path-to-.i-file>");
        System.exit(2);
    }
}
