package compiler.ast;

import compiler.ast.declaration.Declaration;
import compiler.ast.declaration.RoutineDeclaration;
import java.util.ArrayList;
import java.util.List;
import compiler.lexer.TokenType;
import compiler.lexer.Token;
import compiler.parser.Parser;
import compiler.parser.ParserException;

public class Program extends Node {

    public final List<Declaration> declarations;

    private Program(int line, int column, List<Declaration> declarations) {
        super(line, column);
        this.declarations = declarations;
    }

    /** Program : { SimpleDeclaration | RoutineDeclaration } */
    public static Program parse(Parser p) {
        int line = p.getCurrent().line();
        int column = p.getCurrent().column();
        List<Declaration> declarations = new ArrayList<>();

        p.skipSeparators();
        while (!p.check(TokenType.EOF)) {
            Token start = p.getCurrent();
            try {
                if (p.check(TokenType.ROUTINE)) {
                    declarations.add(RoutineDeclaration.parse(p));
                } else {
                    declarations.add(Declaration.parseSimple(p));
                }
            } catch (ParserException error) {
                p.report(error);
                // An invalid token already at a sync point must not be retried forever.
                if (p.getCurrent() == start) p.advance();
                p.synchronizeDeclaration();
            }
            p.skipSeparators();
        }

        return new Program(line, column, declarations);
    }
}
