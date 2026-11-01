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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Reads an I source file and prints its AST, or syntax errors, to the console. */
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
                program = optimize(program);
            }

            System.out.print(printAst ? program.describe() : SourcePrinter.print(program));
        } catch (IOException | LexerException | ParserException error) {
            System.err.println(error.getMessage());
            System.exit(1);
        }
    }

    private static Program optimize(Program program) {
        return program;
    }

    private static void usage() {
        System.err.println("usage: Main [--no-opt] [--ast] <path-to-.i-file>");
        System.exit(2);
    }
}
