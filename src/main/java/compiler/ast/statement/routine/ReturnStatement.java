package compiler.ast.statement.routine;

import compiler.ast.expression.Expression;
import compiler.ast.statement.Statement;
import compiler.lexer.Token;
import compiler.lexer.TokenType;
import compiler.parser.Parser;

public class ReturnStatement extends Statement {

    public final Expression value; // nullable

    private ReturnStatement(int line, int column, Expression value) {
        super(line, column);
        this.value = value;
    }

    /** ReturnStatement : return [ Expression ] */
    public static ReturnStatement parse(Parser p) {
        Token start = p.expect(TokenType.RETURN);
        Expression value = startsExpression(p.getCurrent().type()) ? Expression.parse(p) : null;
        return new ReturnStatement(start.line(), start.column(), value);
    }

    /** Builds a return node from an already-parsed expression (used to desugar "=> Expression"). */
    public static ReturnStatement wrap(int line, int column, Expression value) {
        return new ReturnStatement(line, column, value);
    }

    private static boolean startsExpression(TokenType type) {
        return switch (type) {
            case IDENTIFIER, INTEGER_LITERAL, REAL_LITERAL, CHAR_LITERAL, STRING_LITERAL,
                    TRUE, FALSE, PLUS, MINUS, NOT, LPAREN -> true;
            default -> false;
        };
    }
}
