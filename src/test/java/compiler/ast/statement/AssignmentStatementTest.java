package compiler.ast.statement;

import static org.junit.jupiter.api.Assertions.*;

import compiler.TestPrograms;
import compiler.ast.Program;
import compiler.ast.declaration.RoutineDeclaration;
import compiler.ast.expression.accessor.FieldAccess;
import compiler.ast.expression.accessor.IndexAccess;
import compiler.ast.expression.expression.BinaryExpression;
import compiler.ast.expression.literal.IntegerLiteral;
import compiler.lexer.Lexer;
import compiler.lexer.TokenType;
import compiler.parser.Parser;
import compiler.parser.ParserException;
import org.junit.jupiter.api.Test;

class AssignmentStatementTest {

    @Test
    void assignsToSimpleName() {
        Parser p = new Parser(new Lexer("x := 1"));
        AssignmentStatement statement = assertInstanceOf(AssignmentStatement.class, Statement.parse(p));

        assertEquals("x", statement.target.base);
        assertTrue(statement.target.accessors.isEmpty());
        assertEquals(1, assertInstanceOf(IntegerLiteral.class, statement.value).value);
        assertEquals(TokenType.EOF, p.getCurrent().type());
    }

    @Test
    void assignsToIndexedFieldWithExpressionValue() {
        AssignmentStatement statement = assertInstanceOf(AssignmentStatement.class,
                Statement.parse(new Parser(new Lexer("items[2].price := x + 1"))));

        assertEquals("items", statement.target.base);
        assertInstanceOf(IndexAccess.class, statement.target.accessors.get(0));
        assertEquals("price", assertInstanceOf(FieldAccess.class,
                statement.target.accessors.get(1)).name);
        assertEquals(TokenType.PLUS,
                assertInstanceOf(BinaryExpression.class, statement.value).op);
    }

    @Test
    void worksInsideRoutineBody() {
        Program program = Program.parse(new Parser(new Lexer(
                TestPrograms.read("assignment-in-routine.i"))));
        RoutineDeclaration routine = assertInstanceOf(RoutineDeclaration.class,
                program.declarations.get(0));

        assertEquals(3, routine.body.items.size());
        assertInstanceOf(AssignmentStatement.class, routine.body.items.get(1));
    }

    @Test
    void requiresAssignmentOperatorAndValue() {
        assertThrows(ParserException.class,
                () -> Statement.parse(new Parser(new Lexer("x + 1"))));
        assertThrows(ParserException.class,
                () -> Statement.parse(new Parser(new Lexer("x := "))));
    }
}
