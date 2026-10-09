package compiler.ast.declaration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import compiler.ast.type.PrimitiveTypeNode;
import compiler.ast.type.TypeReferenceNode;
import compiler.ast.expression.literal.IntegerLiteral;
import compiler.lexer.Lexer;
import compiler.parser.Parser;
import compiler.parser.ParserException;

class VariableDeclarationTest {

    @Test
    void typeOnlyForm() {
        Parser p = new Parser(new Lexer("var x: integer"));
        VariableDeclaration decl = VariableDeclaration.parse(p);

        assertEquals("x", decl.name);
        assertInstanceOf(PrimitiveTypeNode.class, decl.type);
        assertEquals(PrimitiveTypeNode.Kind.INTEGER, ((PrimitiveTypeNode) decl.type).kind);
        assertNull(decl.initializer);
    }

    @Test
    void typeReferenceForm() {
        Parser p = new Parser(new Lexer("var p: Person"));
        VariableDeclaration decl = VariableDeclaration.parse(p);

        assertInstanceOf(TypeReferenceNode.class, decl.type);
        assertEquals("Person", ((TypeReferenceNode) decl.type).name);
    }

    @Test
    void missingTypeAndInitializerIsAnError() {
        Parser p = new Parser(new Lexer("var x"));
        assertThrows(ParserException.class, () -> VariableDeclaration.parse(p));
    }

    @Test
    void initializerWithoutExplicitType() {
        VariableDeclaration decl = VariableDeclaration.parse(new Parser(new Lexer("var x is 42")));

        assertNull(decl.type);
        assertEquals(42, assertInstanceOf(IntegerLiteral.class, decl.initializer).value);
    }

    @Test
    void typeAndInitializerTogether() {
        VariableDeclaration decl = VariableDeclaration.parse(
                new Parser(new Lexer("var x: integer is 42")));

        assertInstanceOf(PrimitiveTypeNode.class, decl.type);
        assertEquals(42, assertInstanceOf(IntegerLiteral.class, decl.initializer).value);
    }
}
