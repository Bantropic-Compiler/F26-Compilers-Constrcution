package compiler.ast;

import compiler.ast.declaration.RoutineDeclaration;
import compiler.ast.declaration.TypeDeclaration;
import compiler.ast.declaration.VariableDeclaration;
import compiler.ast.type.RecordTypeNode;
import compiler.lexer.Lexer;
import compiler.lexer.TokenType;
import compiler.parser.Parser;
import java.time.Duration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ParserRecoveryTest {

    @Test
    void collectsIndependentDeclarationErrorsAndKeepsLaterDeclarations() {
        Parser p = new Parser(new Lexer("var first\nvar kept: integer\ntype Broken is\nvar also: integer\n"));

        Program program = Program.parse(p);

        assertEquals(2, p.getErrors().size());
        assertEquals(1, p.getErrors().get(0).line());
        assertEquals(3, p.getErrors().get(1).line());
        assertEquals(2, program.declarations.size());
        assertEquals("kept", ((VariableDeclaration) program.declarations.get(0)).name);
        assertEquals("also", ((VariableDeclaration) program.declarations.get(1)).name);
        assertEquals(TokenType.EOF, p.getCurrent().type());
    }

    @Test
    void recoversInsideRoutineBodyAndContinuesAfterRoutine() {
        Parser p = new Parser(new Lexer("routine work() is\n"
                + "var bad\nvar a: integer\n) stray\nvar b: integer\nend\n"
                + "type After is integer\n"));

        Program program = Program.parse(p);

        assertEquals(2, p.getErrors().size());
        assertEquals(2, p.getErrors().get(0).line());
        assertEquals(4, p.getErrors().get(1).line());
        assertEquals(2, program.declarations.size());
        RoutineDeclaration routine = (RoutineDeclaration) program.declarations.get(0);
        assertEquals(2, routine.body.items.size());
        assertEquals("a", ((VariableDeclaration) routine.body.items.get(0)).name);
        assertEquals("b", ((VariableDeclaration) routine.body.items.get(1)).name);
        assertEquals("After", ((TypeDeclaration) program.declarations.get(1)).name);
    }

    @Test
    void recoversRecordMembersWithoutDiscardingTheType() {
        Parser p = new Parser(new Lexer("type Pair is record\nvar bad\n"
                + "var good: integer\nend\ntype After is integer"));

        Program program = Program.parse(p);

        assertEquals(1, p.getErrors().size());
        assertEquals(2, program.declarations.size());
        RecordTypeNode record = (RecordTypeNode) ((TypeDeclaration) program.declarations.get(0)).type;
        assertEquals(1, record.members.size());
        assertEquals("good", record.members.get(0).name);
    }

    @Test
    void malformedTokenAtSyncPointAndUnclosedRecordReachEof() {
        Parser p = new Parser(new Lexer("end\ntype Broken is record\nvar x: integer"));

        Program program = assertTimeoutPreemptively(Duration.ofSeconds(1), () -> Program.parse(p));

        assertTrue(program.declarations.isEmpty());
        assertEquals(2, p.getErrors().size());
        assertEquals(TokenType.EOF, p.getCurrent().type());
    }

    @Test
    void missingRecordEndLeavesNextDeclarationStarterAvailable() {
        Parser p = new Parser(new Lexer("type Broken is record\n"
                + "var x: integer\ntype After is integer"));

        Program program = Program.parse(p);

        assertEquals(1, p.getErrors().size());
        assertEquals(1, program.declarations.size());
        assertEquals("After", ((TypeDeclaration) program.declarations.get(0)).name);
    }
}
