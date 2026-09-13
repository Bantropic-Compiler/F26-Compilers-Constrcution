package compiler;

import compiler.lexer.Lexer;
import compiler.lexer.Token;
import compiler.lexer.TokenType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Reads a .i source file passed as an argument and prints its token
 * stream (line:column, type, lexeme) — used for the live demo.
 */
public class Main {

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("usage: Main <path-to-.i-file>");
            return;
        }

        String source = Files.readString(Path.of(args[0]));
        Lexer lexer = new Lexer(source);

        Token token;
        do {
            token = lexer.nextToken();
            System.out.printf("%3d:%-3d %-15s %s%n",
                    token.line(), token.column(), token.type(), token.lexeme());
        } while (token.type() != TokenType.EOF);
    }
}
