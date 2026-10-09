package compiler.ast.statement;

import compiler.ast.expression.Expression;
import compiler.ast.expression.ModifiablePrimaryNode;
import compiler.lexer.Token;
import compiler.lexer.TokenType;
import compiler.parser.Parser;

/** ModifiablePrimary := Expression */
public class AssignmentStatement extends Statement {

    public final ModifiablePrimaryNode target;
    public final Expression value;

    private AssignmentStatement(int line, int column, ModifiablePrimaryNode target, Expression value) {
        super(line, column);
        this.target = target;
        this.value = value;
    }

    public static AssignmentStatement parse(Parser p) {
        return parse(p, p.expect(TokenType.IDENTIFIER));
    }

    static AssignmentStatement parse(Parser p, Token name) {
        ModifiablePrimaryNode target = ModifiablePrimaryNode.parse(p, name);
        p.expect(TokenType.ASSIGN);
        Expression value = Expression.parse(p);
        return new AssignmentStatement(target.line, target.column, target, value);
    }
}
