package compiler.ast.expression;

import static org.junit.jupiter.api.Assertions.*;

import compiler.ast.declaration.RoutineDeclaration;
import compiler.ast.declaration.VariableDeclaration;
import compiler.ast.expression.accessor.FieldAccess;
import compiler.ast.expression.accessor.IndexAccess;
import compiler.ast.expression.expression.BinaryExpression;
import compiler.ast.expression.expression.RoutineCallExpression;
import compiler.ast.expression.expression.UnaryExpression;
import compiler.ast.expression.literal.BooleanLiteral;
import compiler.ast.expression.literal.CharLiteral;
import compiler.ast.expression.literal.IntegerLiteral;
import compiler.ast.expression.literal.RealLiteral;
import compiler.ast.expression.literal.StringLiteral;
import compiler.ast.statement.routine.ReturnStatement;
import compiler.ast.type.ArrayTypeNode;
import compiler.ast.type.TypeNode;
import compiler.lexer.Lexer;
import compiler.lexer.TokenType;
import compiler.parser.Parser;
import compiler.parser.ParserException;
import org.junit.jupiter.api.Test;

class ExpressionTest {

    private Expression parseWhole(String source) {
        Parser p = new Parser(new Lexer(source));
        Expression result = Expression.parse(p);
        assertEquals(TokenType.EOF, p.getCurrent().type());
        return result;
    }

    @Test
    void multiplicationBindsMoreTightlyThanAddition() {
        BinaryExpression plus = assertInstanceOf(BinaryExpression.class, parseWhole("2 + 3 * 4"));
        assertEquals(TokenType.PLUS, plus.op);
        assertEquals(2, assertInstanceOf(IntegerLiteral.class, plus.left).value);
        BinaryExpression times = assertInstanceOf(BinaryExpression.class, plus.right);
        assertEquals(TokenType.STAR, times.op);
        assertEquals(3, assertInstanceOf(IntegerLiteral.class, times.left).value);
        assertEquals(4, assertInstanceOf(IntegerLiteral.class, times.right).value);
    }

    @Test
    void parenthesesAndLeftAssociativity() {
        BinaryExpression times = assertInstanceOf(BinaryExpression.class, parseWhole("(2 + 3) * 4"));
        assertEquals(TokenType.STAR, times.op);
        assertEquals(TokenType.PLUS, assertInstanceOf(BinaryExpression.class, times.left).op);

        BinaryExpression outer = assertInstanceOf(BinaryExpression.class, parseWhole("10 - 3 - 2"));
        assertEquals(TokenType.MINUS, outer.op);
        assertEquals(TokenType.MINUS, assertInstanceOf(BinaryExpression.class, outer.left).op);
    }

    @Test
    void comparisonBindsMoreTightlyThanLogicalOperators() {
        BinaryExpression and = assertInstanceOf(BinaryExpression.class, parseWhole("a < b and c = d"));
        assertEquals(TokenType.AND, and.op);
        assertEquals(TokenType.LT, assertInstanceOf(BinaryExpression.class, and.left).op);
        assertEquals(TokenType.EQ, assertInstanceOf(BinaryExpression.class, and.right).op);
    }

    @Test
    void parsesAllLiteralKinds() {
        assertEquals(42, assertInstanceOf(IntegerLiteral.class, parseWhole("42")).value);
        assertEquals(1.5, assertInstanceOf(RealLiteral.class, parseWhole("1.5")).value);
        assertTrue(assertInstanceOf(BooleanLiteral.class, parseWhole("true")).value);
        assertFalse(assertInstanceOf(BooleanLiteral.class, parseWhole("false")).value);
        assertEquals('x', assertInstanceOf(CharLiteral.class, parseWhole("'x'")).codePoint);
        assertEquals("hello", assertInstanceOf(StringLiteral.class, parseWhole("\"hello\"")).value);
    }

    @Test
    void unaryOperatorsFollowTheLiteralOnlyGrammar() {
        UnaryExpression minus = assertInstanceOf(UnaryExpression.class, parseWhole("-5"));
        assertEquals(TokenType.MINUS, minus.op);
        assertEquals(5, assertInstanceOf(IntegerLiteral.class, minus.operand).value);
        assertEquals(TokenType.PLUS, assertInstanceOf(UnaryExpression.class, parseWhole("+5")).op);
        UnaryExpression real = assertInstanceOf(UnaryExpression.class, parseWhole("-1.5"));
        assertEquals(1.5, assertInstanceOf(RealLiteral.class, real.operand).value);
        assertThrows(ParserException.class, () -> parseWhole("not 5"));
        assertThrows(ParserException.class, () -> parseWhole("not true"));
        assertThrows(ParserException.class, () -> parseWhole("not flag"));
        assertThrows(ParserException.class, () -> parseWhole("-value"));
    }

    @Test
    void distinguishesCallFromIndexedAndFieldAccess() {
        RoutineCallExpression call = assertInstanceOf(RoutineCallExpression.class,
                parseWhole("f(1, a + 2)"));
        assertEquals("f", call.name);
        assertEquals(2, call.args.size());
        assertInstanceOf(BinaryExpression.class, call.args.get(1));

        ModifiablePrimaryNode target = assertInstanceOf(ModifiablePrimaryNode.class,
                parseWhole("items[2].price"));
        assertEquals("items", target.base);
        assertEquals(2, target.accessors.size());
        assertEquals(2, assertInstanceOf(IntegerLiteral.class,
                assertInstanceOf(IndexAccess.class, target.accessors.get(0)).index).value);
        assertEquals("price", assertInstanceOf(FieldAccess.class, target.accessors.get(1)).name);

        ModifiablePrimaryNode matrix = assertInstanceOf(ModifiablePrimaryNode.class,
                parseWhole("matrix[i][i]"));
        assertEquals(2, matrix.accessors.size());
        assertInstanceOf(IndexAccess.class, matrix.accessors.get(1));
    }

    @Test
    void expressionsWorkInsideExistingDeclarations() {
        VariableDeclaration variable = VariableDeclaration.parse(new Parser(new Lexer("var x is 2 + 3")));
        assertInstanceOf(BinaryExpression.class, variable.initializer);

        ArrayTypeNode array = assertInstanceOf(ArrayTypeNode.class,
                TypeNode.parse(new Parser(new Lexer("array[2 + 3] integer"))));
        assertInstanceOf(BinaryExpression.class, array.sizeExpr);

        RoutineDeclaration routine = RoutineDeclaration.parse(
                new Parser(new Lexer("routine square(x: integer): integer => x * x")));
        assertEquals(1, routine.body.items.size());
        ReturnStatement result = assertInstanceOf(ReturnStatement.class, routine.body.items.get(0));
        assertEquals(TokenType.STAR, assertInstanceOf(BinaryExpression.class, result.value).op);
    }

    @Test
    void reportsMalformedExpression() {
        assertThrows(ParserException.class, () -> parseWhole("1 +"));
        assertThrows(ParserException.class, () -> parseWhole("f(1,)"));
        assertThrows(ParserException.class, () -> parseWhole("a[2"));
    }
}
