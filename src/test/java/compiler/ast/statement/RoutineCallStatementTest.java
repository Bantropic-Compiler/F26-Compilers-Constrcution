package compiler.ast.statement;

import static org.junit.jupiter.api.Assertions.*;

import compiler.TestPrograms;
import compiler.ast.Program;
import compiler.ast.declaration.RoutineDeclaration;
import compiler.ast.expression.expression.BinaryExpression;
import compiler.ast.expression.literal.IntegerLiteral;
import compiler.ast.statement.routine.RoutineCallStatement;
import compiler.lexer.Lexer;
import compiler.lexer.TokenType;
import compiler.parser.Parser;
import compiler.parser.ParserException;
import org.junit.jupiter.api.Test;

class RoutineCallStatementTest {

    @Test
    void dispatchesCallWithArguments() {
        Parser p = new Parser(new Lexer("show(1, x + 2)"));
        RoutineCallStatement call = assertInstanceOf(RoutineCallStatement.class, Statement.parse(p));

        assertEquals("show", call.name);
        assertEquals(2, call.args.size());
        assertEquals(1, assertInstanceOf(IntegerLiteral.class, call.args.get(0)).value);
        assertInstanceOf(BinaryExpression.class, call.args.get(1));
        assertEquals(TokenType.EOF, p.getCurrent().type());
    }

    @Test
    void acceptsEmptyAndOmittedArgumentLists() {
        for (String source : new String[] { "show()", "show" }) {
            RoutineCallStatement call = assertInstanceOf(RoutineCallStatement.class,
                    Statement.parse(new Parser(new Lexer(source))));
            assertEquals("show", call.name);
            assertTrue(call.args.isEmpty());
        }
    }

    @Test
    void callAndAssignmentShareIdentifierDispatch() {
        Program program = Program.parse(new Parser(new Lexer(
                TestPrograms.read("call-assignment-dispatch.i"))));
        RoutineDeclaration routine = assertInstanceOf(RoutineDeclaration.class,
                program.declarations.get(0));

        assertInstanceOf(RoutineCallStatement.class, routine.body.items.get(1));
        assertInstanceOf(AssignmentStatement.class, routine.body.items.get(2));
        assertInstanceOf(RoutineCallStatement.class, routine.body.items.get(3));
    }

    @Test
    void reportsMalformedArgumentList() {
        assertThrows(ParserException.class,
                () -> Statement.parse(new Parser(new Lexer("show(1,)"))));
        assertThrows(ParserException.class,
                () -> Statement.parse(new Parser(new Lexer("show(1"))));
    }
}
