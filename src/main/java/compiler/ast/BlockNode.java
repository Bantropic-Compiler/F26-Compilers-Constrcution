package compiler.ast;

import compiler.ast.declaration.Declaration;
import compiler.ast.statement.Statement;
import java.util.ArrayList;
import java.util.List;
import compiler.lexer.TokenType;
import compiler.lexer.Token;
import compiler.parser.Parser;
import compiler.parser.ParserException;

/**
 * Body : { SimpleDeclaration | Statement }
 * Reused for routine bodies, loop bodies, and if/then/else branches.
 */
public class BlockNode extends Node {

    public final List<Node> items; // Declaration and/or Statement

    private BlockNode(int line, int column, List<Node> items) {
        super(line, column);
        this.items = items;
    }

    public static BlockNode parse(Parser p) {
        int line = p.getCurrent().line();
        int column = p.getCurrent().column();
        List<Node> items = new ArrayList<>();

        p.skipSeparators();
        while (!p.check(TokenType.END) && !p.check(TokenType.ELSE) && !p.check(TokenType.EOF)) {
            Token start = p.getCurrent();
            boolean declaration = p.check(TokenType.VAR) || p.check(TokenType.TYPE);
            try {
                if (declaration) {
                    items.add(Declaration.parseSimple(p));
                } else {
                    items.add(Statement.parse(p));
                }
            } catch (ParserException error) {
                p.report(error);
                if (p.getCurrent() == start) p.advance();
                if (declaration) p.synchronizeDeclaration();
                else p.synchronizeStatement();
            }
            p.skipSeparators();
        }

        return new BlockNode(line, column, items);
    }

    /** Builds a block directly from already-parsed items (used to desugar "=> Expression"). */
    public static BlockNode wrap(int line, int column, List<Node> items) {
        return new BlockNode(line, column, items);
    }
}
