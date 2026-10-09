package compiler.ast.statement;

import static org.junit.jupiter.api.Assertions.*;

import compiler.TestPrograms;
import compiler.ast.Program;
import compiler.ast.declaration.RoutineDeclaration;
import compiler.ast.expression.expression.BinaryExpression;
import compiler.ast.expression.literal.BooleanLiteral;
import compiler.ast.statement.loop.BreakStatement;
import compiler.ast.statement.loop.ContinueStatement;
import compiler.ast.statement.loop.WhileStatement;
import compiler.lexer.Lexer;
import compiler.lexer.TokenType;
import compiler.parser.Parser;
import compiler.parser.ParserException;
import org.junit.jupiter.api.Test;

class WhileStatementTest {

    @Test
    void parsesConditionAndBody() {
        Parser p = new Parser(new Lexer("while i < 10 loop\n    i := i + 1\nend"));
        WhileStatement loop = assertInstanceOf(WhileStatement.class, Statement.parse(p));

        assertEquals(TokenType.LT,
                assertInstanceOf(BinaryExpression.class, loop.condition).op);
        assertEquals(1, loop.body.items.size());
        assertInstanceOf(AssignmentStatement.class, loop.body.items.get(0));
        assertEquals(TokenType.EOF, p.getCurrent().type());
    }

    @Test
    void handlesNestedBlocksAndLoopControlStatements() {
        Program program = Program.parse(new Parser(new Lexer(
                TestPrograms.read("nested-loop-control.i"))));
        RoutineDeclaration routine = assertInstanceOf(RoutineDeclaration.class,
                program.declarations.get(0));
        WhileStatement loop = assertInstanceOf(WhileStatement.class, routine.body.items.get(0));

        assertTrue(assertInstanceOf(BooleanLiteral.class, loop.condition).value);
        IfStatement branch = assertInstanceOf(IfStatement.class, loop.body.items.get(0));
        assertInstanceOf(BreakStatement.class, branch.thenBody.items.get(0));
        assertInstanceOf(ContinueStatement.class, loop.body.items.get(1));
        assertInstanceOf(PrintStatement.class, routine.body.items.get(1));
    }

    @Test
    void requiresLoopAndEnd() {
        assertThrows(ParserException.class,
                () -> Statement.parse(new Parser(new Lexer("while true print 1 end"))));
        assertThrows(ParserException.class,
                () -> Statement.parse(new Parser(new Lexer("while true loop print 1"))));
    }
}
