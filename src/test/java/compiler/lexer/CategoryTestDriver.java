package compiler.lexer;

import java.util.ArrayList;
import java.util.List;

/** Collects tokens and diagnostics through the public lexer API for tests. */
final class CategoryTestDriver {
    record Result(List<Token> tokens, List<LexerException> errors) {}

    static Result scan(String source) {
        Lexer lexer = new Lexer(source);
        List<Token> tokens = new ArrayList<>();
        List<LexerException> errors = new ArrayList<>();
        while (true) {
            try {
                Token token = lexer.nextToken();
                tokens.add(token);
                if (token.type() == TokenType.EOF) {
                    return new Result(List.copyOf(tokens), List.copyOf(errors));
                }
            } catch (LexerException error) {
                errors.add(error);
                lexer.recover();
            }
        }
    }

    private CategoryTestDriver() {}
}
