package compiler.semantic.type;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class SemanticTypeTest {

    @Test
    void primitiveTypesAreSingletonsWithSourceNames() {
        assertEquals("integer", PrimitiveType.INTEGER.toString());
        assertEquals("real", PrimitiveType.REAL.toString());
        assertEquals("boolean", PrimitiveType.BOOLEAN.toString());
        assertEquals("char", PrimitiveType.CHAR.toString());
        assertSame(PrimitiveType.INTEGER, PrimitiveType.of(PrimitiveType.Kind.INTEGER));
        assertSame(PrimitiveType.REAL, PrimitiveType.of(PrimitiveType.Kind.REAL));
        assertSame(PrimitiveType.BOOLEAN, PrimitiveType.of(PrimitiveType.Kind.BOOLEAN));
        assertSame(PrimitiveType.CHAR, PrimitiveType.of(PrimitiveType.Kind.CHAR));
        assertEquals(PrimitiveType.Kind.CHAR, PrimitiveType.CHAR.kind());
    }

    @Test
    void specialTypesHaveStableNamesAndPredicates() {
        assertEquals("string", StringType.INSTANCE.toString());
        assertEquals("void", VoidType.INSTANCE.toString());
        assertEquals("<error>", ErrorType.INSTANCE.toString());
        assertTrue(ErrorType.INSTANCE.isError());
        assertTrue(VoidType.INSTANCE.isVoid());
        assertFalse(PrimitiveType.INTEGER.isError());
        assertFalse(PrimitiveType.INTEGER.isVoid());
    }

    @Test
    void onlyIntegerAndRealAreNumeric() {
        assertTrue(PrimitiveType.INTEGER.isNumeric());
        assertTrue(PrimitiveType.REAL.isNumeric());
        assertFalse(PrimitiveType.BOOLEAN.isNumeric());
        assertFalse(PrimitiveType.CHAR.isNumeric());
        assertFalse(StringType.INSTANCE.isNumeric());
        assertFalse(new ArrayType(PrimitiveType.INTEGER, 3).isNumeric());
        assertFalse(ErrorType.INSTANCE.isNumeric());
    }

    @Test
    void arrayTypesCompareStructurally() {
        ArrayType first = new ArrayType(PrimitiveType.INTEGER, 5);
        ArrayType same = new ArrayType(PrimitiveType.INTEGER, 5);

        assertEquals(first, same);
        assertEquals(first.hashCode(), same.hashCode());
        assertNotEquals(first, new ArrayType(PrimitiveType.INTEGER, 6));
        assertNotEquals(first, new ArrayType(PrimitiveType.REAL, 5));
        assertNotEquals(first, new ArrayType(PrimitiveType.INTEGER, ArrayType.NO_SIZE));
        assertEquals(new ArrayType(first, 2), new ArrayType(same, 2));
    }

    @Test
    void arrayTypesReportSizeAndElement() {
        ArrayType sized = new ArrayType(PrimitiveType.CHAR, 4);
        ArrayType unsized = new ArrayType(PrimitiveType.CHAR, ArrayType.NO_SIZE);

        assertTrue(sized.hasSize());
        assertEquals(4, sized.size());
        assertSame(PrimitiveType.CHAR, sized.elementType());
        assertFalse(unsized.hasSize());
        assertEquals("array[4] char", sized.toString());
        assertEquals("array[] char", unsized.toString());
        assertEquals("array[2] array[3] real",
                new ArrayType(new ArrayType(PrimitiveType.REAL, 3), 2).toString());
    }

    @Test
    void recordTypesAreComparedByIdentity() {
        RecordType first = new RecordType("P");
        RecordType second = new RecordType("P");
        first.addField("x", PrimitiveType.INTEGER);
        second.addField("x", PrimitiveType.INTEGER);

        assertEquals(first, first);
        assertNotEquals(first, second);
    }

    @Test
    void recordTypesKeepFieldsInDeclarationOrderAndFirstDuplicateWins() {
        RecordType record = new RecordType("P");
        record.addField("b", PrimitiveType.INTEGER);
        record.addField("a", StringType.INSTANCE);
        record.addField("b", PrimitiveType.REAL);

        assertEquals(List.of("b", "a"), List.copyOf(record.fields().keySet()));
        assertSame(PrimitiveType.INTEGER, record.fieldType("b"));
        assertNull(record.fieldType("missing"));
        assertThrows(UnsupportedOperationException.class,
                () -> record.fields().put("c", PrimitiveType.CHAR));
    }

    @Test
    void recordNameFallsBackToRecordWhenAnonymous() {
        assertEquals("Person", new RecordType("Person").toString());
        assertEquals("record", new RecordType(null).toString());
        assertEquals("Person", new RecordType("Person").name());
        assertNull(new RecordType(null).name());
    }
}
