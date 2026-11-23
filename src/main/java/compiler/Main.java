package compiler;

import compiler.ast.Program;
import compiler.codegen.CodeGenerator;
import compiler.codegen.Jasmin;
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

/**
 * Parses, checks and optionally optimizes I source. Run from the repository root.
 * <pre>{@code
 * mvn compile
 *
 * # Print the source or AST, with or without optimization
 * java -cp target/classes compiler.Main prog-examples/01-print-number.i
 * java -cp target/classes compiler.Main --no-opt prog-examples/01-print-number.i
 * java -cp target/classes compiler.Main --ast prog-examples/01-print-number.i
 * java -cp target/classes compiler.Main --no-opt --ast prog-examples/01-print-number.i
 *
 * # Generate Program.j and Program.class, then call the compiled routine (prints 42)
 * java -cp target/classes compiler.Main -o target/demo/01-print-number prog-examples/01-print-number.i
 * java -cp target/demo/01-print-number Program show 42
 */
public class Main {

    public static void main(String[] args) {
        boolean optimize = true;
        boolean printAst = false;
        String path = null;
        Path output = null;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("--no-opt")) {
                optimize = false;
            } else if (arg.equals("--ast")) {
                printAst = true;
            } else if (arg.equals("-o")) {
                if (output != null || ++i == args.length || args[i].startsWith("-")) {
                    usage();
                    return;
                }
                output = Path.of(args[i]);
            } else if (arg.startsWith("-") || path != null) {
                usage();
                return;
            } else {
                path = arg;
            }
        }
        if (path == null || (printAst && output != null)) {
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

            if (output != null) {
                Jasmin.assemble(new CodeGenerator(analysis.model()).generate(program), output);
            } else {
                System.out.print(printAst ? program.describe() : SourcePrinter.print(program));
            }
        } catch (IOException | LexerException | ParserException error) {
            System.err.println(error.getMessage());
            System.exit(1);
        }
    }

    private static void usage() {
        System.err.println("usage: Main [--no-opt] [--ast | -o <dir>] <path-to-.i-file>");
        System.exit(2);
    }
}
