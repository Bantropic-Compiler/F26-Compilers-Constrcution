package compiler.ast.statement;

import static org.junit.jupiter.api.Assertions.*;

import compiler.ast.Program;
import compiler.ast.declaration.RoutineDeclaration;
import compiler.ast.expression.ModifiablePrimaryNode;
import compiler.ast.expression.literal.IntegerLiteral;
import compiler.ast.statement.loop.ForStatement;
import compiler.lexer.Lexer;
import compiler.lexer.TokenType;
import compiler.parser.Parser;
import compiler.parser.ParserException;
import org.junit.jupiter.api.Test;

class ForStatementTest {

    @Test
    void parsesNumericRange() {
        Parser p = new Parser(new Lexer("for i in 1..n loop\n    print i\nend"));
        ForStatement loop = assertInstanceOf(ForStatement.class, Statement.parse(p));

        assertEquals("i", loop.loopVar);
        assertEquals(1, assertInstanceOf(IntegerLiteral.class, loop.rangeStart).value);
        assertEquals("n", assertInstanceOf(ModifiablePrimaryNode.class, loop.rangeEnd).base);
        assertFalse(loop.reverse);
        assertInstanceOf(PrintStatement.class, loop.body.items.get(0));
        assertEquals(TokenType.EOF, p.getCurrent().type());
    }

    @Test
    void parsesArrayIterationAndReverse() {
        ForStatement loop = assertInstanceOf(ForStatement.class,
                Statement.parse(new Parser(new Lexer("for value in values reverse loop\n"
                        + "    print value\nend"))));

        assertEquals("value", loop.loopVar);
        assertEquals("values", assertInstanceOf(ModifiablePrimaryNode.class, loop.rangeStart).base);
        assertNull(loop.rangeEnd);
        assertTrue(loop.reverse);
    }

    @Test
    void parsesReverseRangeInsideRoutine() {
        Program program = Program.parse(new Parser(new Lexer("""
                routine countdown() is
                    for i in 5..1 reverse loop
                        print i
                    end
                    print 0
                end
                """)));
        RoutineDeclaration routine = assertInstanceOf(RoutineDeclaration.class,
                program.declarations.get(0));
        ForStatement loop = assertInstanceOf(ForStatement.class, routine.body.items.get(0));

        assertTrue(loop.reverse);
        assertEquals(1, assertInstanceOf(IntegerLiteral.class, loop.rangeEnd).value);
        assertInstanceOf(PrintStatement.class, routine.body.items.get(1));
    }

    @Test
    void rejectsMalformedHeaderAndMissingEnd() {
        assertThrows(ParserException.class,
                () -> Statement.parse(new Parser(new Lexer("for i 1..3 loop end"))));
        assertThrows(ParserException.class,
                () -> Statement.parse(new Parser(new Lexer("for i in 1.. loop end"))));
        assertThrows(ParserException.class,
                () -> Statement.parse(new Parser(new Lexer("for i in 1..3 print i end"))));
        assertThrows(ParserException.class,
                () -> Statement.parse(new Parser(new Lexer("for i in 1..3 loop print i"))));
    }
}
