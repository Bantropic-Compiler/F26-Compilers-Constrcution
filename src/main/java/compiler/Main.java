package compiler;

import compiler.ast.Program;
import compiler.lexer.Lexer;
import compiler.lexer.LexerException;
import compiler.parser.Parser;
import compiler.parser.ParserException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Reads an I source file and prints its AST, or syntax errors, to the console. */
public class Main {

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("usage: Main <path-to-.i-file>");
            System.exit(2);
            return;
        }

        try {
            String source = Files.readString(Path.of(args[0]));
            Parser parser = new Parser(new Lexer(source));
            Program program = Program.parse(parser);

            if (!parser.getErrors().isEmpty()) {
                for (ParserException error : parser.getErrors()) {
                    System.err.println(error.getMessage());
                }
                System.exit(1);
                return;
            }

            System.out.print(program.describe());
        } catch (IOException | LexerException | ParserException error) {
            System.err.println(error.getMessage());
            System.exit(1);
        }
    }
}
