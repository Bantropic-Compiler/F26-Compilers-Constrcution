package compiler.ast.declaration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

import compiler.ast.type.ArrayTypeNode;
import compiler.ast.type.PrimitiveTypeNode;
import compiler.ast.type.RecordTypeNode;
import compiler.ast.type.StringTypeNode;
import compiler.lexer.Lexer;
import compiler.parser.Parser;

class TypeDeclarationTest {

    @Test
    void aliasToPrimitiveType() {
        Parser p = new Parser(new Lexer("type Count is integer"));
        TypeDeclaration decl = TypeDeclaration.parse(p);

        assertEquals("Count", decl.name);
        assertInstanceOf(PrimitiveTypeNode.class, decl.type);
    }

    @Test
    void aliasToRecordType() {
        Parser p = new Parser(new Lexer("type Item is record\n    var price: integer\nend"));
        TypeDeclaration decl = TypeDeclaration.parse(p);

        assertInstanceOf(RecordTypeNode.class, decl.type);
        RecordTypeNode record = (RecordTypeNode) decl.type;
        assertEquals(1, record.members.size());
        assertEquals("price", record.members.get(0).name);
    }

    @Test
    void aliasToSizelessArrayType() {
        Parser p = new Parser(new Lexer("type IntArray is array[] integer"));
        TypeDeclaration decl = TypeDeclaration.parse(p);

        assertInstanceOf(ArrayTypeNode.class, decl.type);
        ArrayTypeNode array = (ArrayTypeNode) decl.type;
        assertNull(array.sizeExpr);
        assertInstanceOf(PrimitiveTypeNode.class, array.elementType);
    }

    @Test
    void aliasToStringType() {
        Parser p = new Parser(new Lexer("type Name is string"));
        TypeDeclaration decl = TypeDeclaration.parse(p);

        assertInstanceOf(StringTypeNode.class, decl.type);
    }

    // sized array types ("array[3] integer") need Expression.parse().
}
