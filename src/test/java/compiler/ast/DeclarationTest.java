package compiler.ast;

import compiler.lexer.Lexer;
import compiler.parser.Parser;
import compiler.parser.ParserException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DeclarationTest {

    @Test
    void dispatchesVarToVariableDeclaration() {
        Parser p = new Parser(new Lexer("var x: integer"));
        assertInstanceOf(VariableDeclaration.class, Declaration.parseSimple(p));
    }

    @Test
    void dispatchesTypeToTypeDeclaration() {
        Parser p = new Parser(new Lexer("type T is integer"));
        assertInstanceOf(TypeDeclaration.class, Declaration.parseSimple(p));
    }

    @Test
    void rejectsNonDeclarationToken() {
        Parser p = new Parser(new Lexer("while"));
        assertThrows(ParserException.class, () -> Declaration.parseSimple(p));
    }
}
