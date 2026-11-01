package compiler.semantic;

import compiler.lexer.TokenType;

public final class Operators {

    private Operators() {
    }

    public static String symbol(TokenType op) {
        return switch (op) {
            case PLUS -> "+";
            case MINUS -> "-";
            case STAR -> "*";
            case SLASH -> "/";
            case PERCENT -> "%";
            case LT -> "<";
            case LE -> "<=";
            case GT -> ">";
            case GE -> ">=";
            case EQ -> "=";
            case NEQ -> "/=";
            case AND -> "and";
            case OR -> "or";
            case XOR -> "xor";
            case NOT -> "not";
            default -> op.name().toLowerCase();
        };
    }

    public static int precedence(TokenType op) {
        return switch (op) {
            case AND, OR, XOR -> 1;
            case LT, LE, GT, GE, EQ, NEQ -> 2;
            case PLUS, MINUS -> 3;
            case STAR, SLASH, PERCENT -> 4;
            default -> throw new IllegalArgumentException("not a binary operator: " + op);
        };
    }
}
