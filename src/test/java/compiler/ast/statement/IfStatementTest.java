package compiler.ast.statement;

import static org.junit.jupiter.api.Assertions.*;

import compiler.TestPrograms;
import compiler.ast.declaration.RoutineDeclaration;
import compiler.ast.declaration.VariableDeclaration;
import compiler.ast.expression.expression.BinaryExpression;
import compiler.ast.expression.literal.BooleanLiteral;
import compiler.ast.statement.routine.ReturnStatement;
import compiler.lexer.Lexer;
import compiler.lexer.TokenType;
import compiler.parser.Parser;
import compiler.parser.ParserException;
import org.junit.jupiter.api.Test;

class IfStatementTest {

    @Test
    void parsesConditionAndThenBodyWithoutElse() {
        Parser p = new Parser(new Lexer("if x > 0 then\n    print x\nend"));
        IfStatement statement = assertInstanceOf(IfStatement.class, Statement.parse(p));

        assertEquals(TokenType.GT,
                assertInstanceOf(BinaryExpression.class, statement.condition).op);
        assertEquals(1, statement.thenBody.items.size());
        assertInstanceOf(PrintStatement.class, statement.thenBody.items.get(0));
        assertNull(statement.elseBody);
        assertEquals(TokenType.EOF, p.getCurrent().type());
    }

    @Test
    void parsesElseAndNestedIfWithinRoutine() {
        Parser p = new Parser(new Lexer(TestPrograms.read("nested-if.i")));
        RoutineDeclaration routine = RoutineDeclaration.parse(p);
        IfStatement outer = assertInstanceOf(IfStatement.class, routine.body.items.get(0));

        assertTrue(assertInstanceOf(BooleanLiteral.class, outer.condition).value);
        assertInstanceOf(VariableDeclaration.class, outer.thenBody.items.get(0));
        IfStatement inner = assertInstanceOf(IfStatement.class, outer.thenBody.items.get(1));
        assertInstanceOf(PrintStatement.class, inner.thenBody.items.get(0));
        assertInstanceOf(PrintStatement.class, inner.elseBody.items.get(0));
        assertInstanceOf(ReturnStatement.class, outer.elseBody.items.get(0));
        p.skipSeparators();
        assertEquals(TokenType.EOF, p.getCurrent().type());
    }

    @Test
    void missingThenOrEndIsAnError() {
        assertThrows(ParserException.class,
                () -> Statement.parse(new Parser(new Lexer("if true print 1 end"))));
        assertThrows(ParserException.class,
                () -> Statement.parse(new Parser(new Lexer("if true then print 1"))));
    }
}
