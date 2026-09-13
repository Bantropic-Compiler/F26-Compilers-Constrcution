package compiler.lexer;

import java.util.ArrayList;
import java.util.List;

/** Test-only composition of stage 2 scanners. Production integration is stage 4. */
final class CategoryTestDriver {
    record Result(List<Token> tokens, List<LexerException> errors) {}

    static Result scan(String source) {
        Lexer lexer = new Lexer(source);
        SourceReader reader = lexer.reader();
        List<Token> tokens = new ArrayList<>();
        List<LexerException> errors = new ArrayList<>();
        while (true) {
            lexer.skipWhitespace();
            if (reader.isAtEnd()) {
                tokens.add(new Token(TokenType.EOF, "", reader.line(), reader.column(), null));
                return new Result(List.copyOf(tokens), List.copyOf(errors));
            }
            try {
                char c = reader.peek();
                Token token;
                if (Character.isLetter(c) || c == '_') token = lexer.scanIdentifierOrKeyword();
                else if (c >= '0' && c <= '9') token = lexer.scanNumber();
                else token = switch (c) {
                    case '"' -> lexer.scanString();
                    case '\'' -> lexer.scanChar();
                    case '\r', '\n' -> lexer.scanNewline();
                    case ';' -> lexer.scanSeparator();
                    default -> lexer.scanOperatorOrDelimiter();
                };
                if (token != null) tokens.add(token);
            } catch (LexerException error) {
                errors.add(error);
                lexer.recover();
            }
        }
    }

    private CategoryTestDriver() {}
}
