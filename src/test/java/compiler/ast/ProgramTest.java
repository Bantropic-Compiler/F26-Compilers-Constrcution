package compiler.ast;

import compiler.lexer.Lexer;
import compiler.parser.Parser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProgramTest {

    @Test
    void parsesMultipleDeclarationsSeparatedByNewlines() {
        Parser p = new Parser(new Lexer("type Count is integer\nvar x: Count\n"));
        Program program = Program.parse(p);

        assertEquals(2, program.declarations.size());
        assertInstanceOf(TypeDeclaration.class, program.declarations.get(0));
        assertInstanceOf(VariableDeclaration.class, program.declarations.get(1));
    }

    @Test
    void parsesForwardRoutineDeclaration() {
        Parser p = new Parser(new Lexer("routine isOdd(n: integer): boolean"));
        Program program = Program.parse(p);

        assertEquals(1, program.declarations.size());
        RoutineDeclaration routine = (RoutineDeclaration) program.declarations.get(0);
        assertEquals("isOdd", routine.name);
        assertNull(routine.body);
    }

    @Test
    void skipsBlankLinesBetweenDeclarations() {
        Parser p = new Parser(new Lexer("\n\ntype A is integer\n\n\ntype B is integer\n"));
        Program program = Program.parse(p);

        assertEquals(2, program.declarations.size());
    }

    @Test
    void emptyProgramHasNoDeclarations() {
        Parser p = new Parser(new Lexer(""));
        Program program = Program.parse(p);

        assertTrue(program.declarations.isEmpty());
    }
}
