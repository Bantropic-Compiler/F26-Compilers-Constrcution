package compiler.ast;

import compiler.ast.declaration.Declaration;
import compiler.ast.statement.Statement;
import java.util.ArrayList;
import java.util.List;
import compiler.lexer.TokenType;
import compiler.parser.Parser;

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
            if (p.check(TokenType.VAR) || p.check(TokenType.TYPE)) {
                items.add(Declaration.parseSimple(p));
            } else {
                items.add(Statement.parse(p));
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
