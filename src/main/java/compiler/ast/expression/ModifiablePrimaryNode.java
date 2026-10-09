package compiler.ast.expression;

import java.util.ArrayList;
import java.util.List;

import compiler.ast.expression.accessor.FieldAccess;
import compiler.ast.expression.accessor.IndexAccess;
import compiler.lexer.Token;
import compiler.lexer.TokenType;
import compiler.parser.Parser;

/** An identifier followed by zero or more field and index accesses. */
public class ModifiablePrimaryNode extends Expression {

    public final String base;
    public final List<Accessor> accessors;

    private ModifiablePrimaryNode(int line, int column, String base, List<Accessor> accessors) {
        super(line, column);
        this.base = base;
        this.accessors = List.copyOf(accessors);
    }

    public static ModifiablePrimaryNode parse(Parser p) {
        return parse(p, p.expect(TokenType.IDENTIFIER));
    }

    static ModifiablePrimaryNode parse(Parser p, Token name) {
        List<Accessor> accessors = new ArrayList<>();
        while (p.check(TokenType.DOT) || p.check(TokenType.LBRACKET)) {
            accessors.add(p.check(TokenType.DOT) ? FieldAccess.parse(p) : IndexAccess.parse(p));
        }
        return new ModifiablePrimaryNode(name.line(), name.column(), (String) name.value(), accessors);
    }
}
