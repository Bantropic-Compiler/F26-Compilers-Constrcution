package compiler.ast;

import compiler.lexer.Lexer;
import compiler.parser.Parser;
import compiler.parser.ParserException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TypeNodeTest {

    @Test
    void dispatchesToPrimitiveTypes() {
        for (String src : new String[]{"integer", "real", "boolean", "char"}) {
            Parser p = new Parser(new Lexer(src));
            assertInstanceOf(PrimitiveTypeNode.class, TypeNode.parse(p));
        }
    }

    @Test
    void dispatchesToStringType() {
        Parser p = new Parser(new Lexer("string"));
        assertInstanceOf(StringTypeNode.class, TypeNode.parse(p));
    }

    @Test
    void dispatchesToArrayType() {
        Parser p = new Parser(new Lexer("array[] integer"));
        assertInstanceOf(ArrayTypeNode.class, TypeNode.parse(p));
    }

    @Test
    void dispatchesToRecordType() {
        Parser p = new Parser(new Lexer("record\nend"));
        assertInstanceOf(RecordTypeNode.class, TypeNode.parse(p));
    }

    @Test
    void dispatchesToTypeReference() {
        Parser p = new Parser(new Lexer("Point"));
        TypeNode type = TypeNode.parse(p);

        assertInstanceOf(TypeReferenceNode.class, type);
        assertEquals("Point", ((TypeReferenceNode) type).name);
    }

    @Test
    void rejectsNonTypeToken() {
        Parser p = new Parser(new Lexer(":="));
        assertThrows(ParserException.class, () -> TypeNode.parse(p));
    }

    @Test
    void emptyRecordHasNoMembers() {
        Parser p = new Parser(new Lexer("record\nend"));
        RecordTypeNode record = (RecordTypeNode) TypeNode.parse(p);

        assertTrue(record.members.isEmpty());
    }

    @Test
    void recordWithMultipleMembersSeparatedByNewlines() {
        Parser p = new Parser(new Lexer("record\n    var x: integer\n    var y: integer\nend"));
        RecordTypeNode record = (RecordTypeNode) TypeNode.parse(p);

        assertEquals(2, record.members.size());
        assertEquals("x", record.members.get(0).name);
        assertEquals("y", record.members.get(1).name);
    }

    @Test
    void arrayOfArray() {
        Parser p = new Parser(new Lexer("array[] array[] integer"));
        ArrayTypeNode outer = (ArrayTypeNode) TypeNode.parse(p);

        assertInstanceOf(ArrayTypeNode.class, outer.elementType);
    }
}
