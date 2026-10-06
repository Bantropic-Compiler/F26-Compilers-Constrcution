package compiler.ast;

import java.util.ArrayList;
import java.util.List;

import compiler.lexer.TokenType;
import compiler.parser.Parser;

public class Program extends Node {

    public final List<Declaration> declarations;

    private Program(int line, int column, List<Declaration> declarations) {
        super(line, column);
        this.declarations = declarations;
    }

    public static Program parse(Parser p) {
        int line = p.getCurrent().line();
        int column = p.getCurrent().column();
        List<Declaration> declarations = new ArrayList<>();

        p.skipSeparators();
        while (!p.check(TokenType.EOF)) {
            if (p.check(TokenType.ROUTINE)) {
                declarations.add(RoutineDeclaration.parse(p));
            } else {
                declarations.add(Declaration.parseSimple(p));
            }
            p.skipSeparators();
        }

        return new Program(line, column, declarations);
    }
}
