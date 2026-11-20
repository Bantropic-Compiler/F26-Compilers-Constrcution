package compiler.codegen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import compiler.semantic.type.ArrayType;
import compiler.semantic.type.PrimitiveType;
import compiler.semantic.type.RecordType;
import compiler.semantic.type.StringType;
import compiler.semantic.type.VoidType;
import java.util.List;
import org.junit.jupiter.api.Test;

class JvmTest {

    private final Jvm jvm = new Jvm();

    @Test
    void descriptors() {
        assertEquals("I", jvm.descriptor(PrimitiveType.BOOLEAN));
        assertEquals("D", jvm.descriptor(PrimitiveType.REAL));
        assertEquals("Ljava/lang/String;", jvm.descriptor(StringType.INSTANCE));
        assertEquals("[[D", jvm.descriptor(new ArrayType(new ArrayType(PrimitiveType.REAL, 2), 3)));
        assertEquals("(ID)V", jvm.methodDescriptor(List.of(PrimitiveType.INTEGER, PrimitiveType.REAL), VoidType.INSTANCE));
    }

    @Test
    void recordsGetClassesInOrderOfFirstUse() {
        RecordType first = new RecordType("A");
        RecordType second = new RecordType("B");
        assertEquals("LRec1;", jvm.descriptor(first));
        assertEquals("LRec2;", jvm.descriptor(second));
        assertEquals("LRec1;", jvm.descriptor(first));
        assertEquals(List.of(first, second), jvm.records());
    }

    @Test
    void opcodePrefixes() {
        assertEquals("d", jvm.prefix(PrimitiveType.REAL));
        assertEquals("i", jvm.prefix(PrimitiveType.CHAR));
        assertEquals("a", jvm.prefix(StringType.INSTANCE));
    }

    @Test
    void namesAndStrings() {
        assertEquals("g_x", jvm.globalName("x"));
        assertEquals(false, jvm.globalName("x").equals(jvm.methodName("x")));
        assertEquals("\"a\\u0022b\\u000a\\u043f\"", jvm.quote("a\"b\n\u043f"));
    }
}
