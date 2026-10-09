package compiler.ast.expression;

import compiler.ast.Node;
import compiler.ast.expression.expression.BinaryExpression;
import compiler.ast.expression.expression.RoutineCallExpression;
import compiler.ast.expression.expression.UnaryExpression;
import compiler.ast.expression.literal.BooleanLiteral;
import compiler.ast.expression.literal.CharLiteral;
import compiler.ast.expression.literal.IntegerLiteral;
import compiler.ast.expression.literal.RealLiteral;
import compiler.ast.expression.literal.StringLiteral;
import compiler.lexer.Token;
import compiler.lexer.TokenType;
import compiler.parser.Parser;
import compiler.parser.ParserException;

/** An expression is parsed from its operators, from lowest to highest precedence. */
public abstract class Expression extends Node {

    protected Expression(int line, int column) {
        super(line, column);
    }

    public static Expression parse(Parser p) {
        return parseLogical(p);
    }

    private static Expression parseLogical(Parser p) {
        Expression left = parseRelation(p);
        while (p.check(TokenType.AND) || p.check(TokenType.OR) || p.check(TokenType.XOR)) {
            Token op = p.advance();
            left = BinaryExpression.wrap(op, left, parseRelation(p));
        }
        return left;
    }

    private static Expression parseRelation(Parser p) {
        Expression left = parseAdditive(p);
        if (isRelation(p.getCurrent().type())) {
            Token op = p.advance();
            left = BinaryExpression.wrap(op, left, parseAdditive(p));
        }
        return left;
    }

    private static boolean isRelation(TokenType type) {
        return switch (type) {
            case LT, LE, GT, GE, EQ, NEQ -> true;
            default -> false;
        };
    }

    private static Expression parseAdditive(Parser p) {
        Expression left = parseMultiplicative(p);
        while (p.check(TokenType.PLUS) || p.check(TokenType.MINUS)) {
            Token op = p.advance();
            left = BinaryExpression.wrap(op, left, parseMultiplicative(p));
        }
        return left;
    }

    private static Expression parseMultiplicative(Parser p) {
        Expression left = parsePrimary(p);
        while (p.check(TokenType.STAR) || p.check(TokenType.SLASH) || p.check(TokenType.PERCENT)) {
            Token op = p.advance();
            left = BinaryExpression.wrap(op, left, parsePrimary(p));
        }
        return left;
    }

    private static Expression parsePrimary(Parser p) {
        Token token = p.getCurrent();
        return switch (token.type()) {
            case INTEGER_LITERAL -> IntegerLiteral.parse(p);
            case REAL_LITERAL -> RealLiteral.parse(p);
            case TRUE, FALSE -> BooleanLiteral.parse(p);
            case CHAR_LITERAL -> CharLiteral.parse(p);
            case STRING_LITERAL -> StringLiteral.parse(p);
            case PLUS, MINUS -> UnaryExpression.parse(p);
            case IDENTIFIER -> {
                p.advance();
                yield p.check(TokenType.LPAREN)
                        ? RoutineCallExpression.parse(p, token)
                        : ModifiablePrimaryNode.parse(p, token);
            }
            case LPAREN -> {
                p.advance();
                Expression inner = parse(p);
                p.expect(TokenType.RPAREN);
                yield inner;
            }
            default -> throw new ParserException(
                    "expected an expression but found " + token.type(),
                    token.line(), token.column());
        };
    }
}
